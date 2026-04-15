package com.gray.hospital.controller;

import com.gray.hospital.controller.dto.NurseViewRow;
import com.gray.hospital.controller.dto.WeeklyRosterRow;
import com.gray.hospital.entity.Doctor;
import com.gray.hospital.entity.Nurse;
import com.gray.hospital.repository.DoctorRepository;
import com.gray.hospital.repository.NurseRepository;
import com.gray.hospital.service.NurseService;
import com.gray.hospital.service.RosterService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NurseControllerTest {

    @Mock
    private NurseRepository nurseRepository;

    @Mock
    private DoctorRepository doctorRepository;

    @Mock
    private NurseService nurseService;

    @Mock
    private RosterService rosterService;

    @InjectMocks
    private NurseController nurseController;

    @Test
    void profileIncludesAssignedDoctorDetails() {
        Doctor doctor = new Doctor();
        doctor.setDoctorId(9L);
        doctor.setName("Dr. Kavya Rao");

        Nurse nurse = new Nurse();
        nurse.setNurseId(4L);
        nurse.setName("Sneha Iyer");
        nurse.setEmail("sneha@gray.com");
        nurse.setPhone("9999999999");
        nurse.setRole("NURSE");
        nurse.setSalary(new BigDecimal("25000.00"));
        nurse.setDoctor(doctor);

        when(nurseRepository.findById(4L)).thenReturn(Optional.of(nurse));

        NurseViewRow row = nurseController.getProfile(4L);

        assertEquals(4L, row.nurseId());
        assertEquals(9L, row.doctorId());
        assertEquals("Dr. Kavya Rao", row.doctorName());
        assertEquals("25000.00", row.salary());
    }

    @Test
    void weeklyRosterReturnsEmptyListWhenNurseHasNoAssignedDoctor() {
        Nurse nurse = new Nurse();
        nurse.setNurseId(5L);

        when(nurseRepository.findById(5L)).thenReturn(Optional.of(nurse));

        List<WeeklyRosterRow> rows = nurseController.getWeeklyRosterForAssignedDoctor(5L, "2026-04-14");

        assertTrue(rows.isEmpty());
    }

    @Test
    void weeklyRosterDelegatesToAssignedDoctorRoster() {
        Doctor doctor = new Doctor();
        doctor.setDoctorId(2L);

        Nurse nurse = new Nurse();
        nurse.setNurseId(6L);
        nurse.setDoctor(doctor);

        WeeklyRosterRow row = new WeeklyRosterRow(
                "2026-04-14",
                "2026-04-20",
                "2026-04-15",
                2L,
                "Dr. Arjun Mehta",
                "REGULAR",
                1L,
                "09:00",
                "17:00",
                6L,
                "Rahul Paul"
        );

        when(nurseRepository.findById(6L)).thenReturn(Optional.of(nurse));
        when(rosterService.getWeeklyRosterViewForDoctor(LocalDate.parse("2026-04-14"), 2L))
                .thenReturn(List.of(row));

        List<WeeklyRosterRow> rows = nurseController.getWeeklyRosterForAssignedDoctor(6L, "2026-04-14");

        assertEquals(1, rows.size());
        assertEquals("Dr. Arjun Mehta", rows.getFirst().doctorName());
        verify(rosterService).getWeeklyRosterViewForDoctor(LocalDate.parse("2026-04-14"), 2L);
    }

    @Test
    void assignNurseRequiresHeadNurseSessionRole() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.getSession().setAttribute("role", "NURSE");

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> nurseController.assignNurse(1L, 2L, request));

        assertEquals("Only Head Nurse can assign", exception.getMessage());
    }
}
