package com.gray.hospital.service;

import com.gray.hospital.controller.dto.WeeklyRosterRow;
import com.gray.hospital.entity.Doctor;
import com.gray.hospital.entity.Nurse;
import com.gray.hospital.entity.Roster;
import com.gray.hospital.repository.DoctorRepository;
import com.gray.hospital.repository.NurseRepository;
import com.gray.hospital.repository.RosterRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RosterServiceTest {

    @Mock
    private RosterRepository rosterRepository;

    @Mock
    private DoctorRepository doctorRepository;

    @Mock
    private NurseRepository nurseRepository;

    @Mock
    private RosterPublicationService rosterPublicationService;

    @InjectMocks
    private RosterService rosterService;

    @Test
    void addRosterUsesDefaultNineToFiveShiftWhenTimesAreBlank() {
        Doctor doctor = new Doctor();
        doctor.setDoctorId(1L);

        when(rosterRepository.existsByDoctorDoctorIdAndDate(1L, LocalDate.parse("2026-04-15"))).thenReturn(false);
        when(rosterRepository.existsByRoomIdAndDate(3L, LocalDate.parse("2026-04-15"))).thenReturn(false);
        when(doctorRepository.findById(1L)).thenReturn(Optional.of(doctor));
        when(rosterRepository.save(any(Roster.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Roster roster = rosterService.addRoster(1L, 3L, LocalDate.parse("2026-04-15"), null, "");

        assertEquals(LocalTime.of(9, 0), roster.getStartTime());
        assertEquals(LocalTime.of(17, 0), roster.getEndTime());
        assertEquals(3L, roster.getRoomId());
    }

    @Test
    void addRosterRejectsNonStandardStartTime() {
        when(rosterRepository.existsByDoctorDoctorIdAndDate(1L, LocalDate.parse("2026-04-15"))).thenReturn(false);
        when(rosterRepository.existsByRoomIdAndDate(2L, LocalDate.parse("2026-04-15"))).thenReturn(false);
        when(doctorRepository.findById(1L)).thenReturn(Optional.of(new Doctor()));

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> rosterService.addRoster(1L, 2L, LocalDate.parse("2026-04-15"), "10:00", "17:00"));

        assertEquals("Regular doctor roster must be 09:00-17:00 or 20:00-08:00", exception.getMessage());
    }

    @Test
    void weeklyRosterViewIncludesAssignedNurse() {
        Doctor doctor = new Doctor();
        doctor.setDoctorId(4L);
        doctor.setName("Dr. Rohan Sen");
        doctor.setDoctorType("REGULAR");

        Roster roster = new Roster();
        roster.setDoctor(doctor);
        roster.setRoomId(7L);
        roster.setDate(LocalDate.parse("2026-04-16"));
        roster.setStartTime(LocalTime.of(9, 0));
        roster.setEndTime(LocalTime.of(17, 0));

        Nurse nurse = new Nurse();
        nurse.setNurseId(11L);
        nurse.setName("Anita Roy");

        when(rosterRepository.findByDateBetween(LocalDate.parse("2026-04-14"), LocalDate.parse("2026-04-20")))
                .thenReturn(List.of(roster));
        when(nurseRepository.findByDoctorDoctorId(4L)).thenReturn(Optional.of(nurse));

        List<WeeklyRosterRow> rows = rosterService.getWeeklyRosterView(LocalDate.parse("2026-04-14"));

        assertEquals(1, rows.size());
        assertEquals("2026-04-14", rows.getFirst().weekStart());
        assertEquals("Dr. Rohan Sen", rows.getFirst().doctorName());
        assertEquals("Anita Roy", rows.getFirst().nurseName());
    }
}
