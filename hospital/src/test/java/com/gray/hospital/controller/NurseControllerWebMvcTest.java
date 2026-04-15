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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(NurseController.class)
class NurseControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private NurseRepository nurseRepository;

    @MockBean
    private DoctorRepository doctorRepository;

    @MockBean
    private NurseService nurseService;

    @MockBean
    private RosterService rosterService;

    @Test
    void profileEndpointReturnsAssignedDoctorDetails() throws Exception {
        Doctor doctor = new Doctor();
        doctor.setDoctorId(2L);
        doctor.setName("Dr. Kavya Rao");

        Nurse nurse = new Nurse();
        nurse.setNurseId(3L);
        nurse.setName("Sneha Iyer");
        nurse.setDoctor(doctor);

        when(nurseRepository.findById(3L)).thenReturn(Optional.of(nurse));

        mockMvc.perform(get("/nurses/3/profile"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nurseId").value(3))
                .andExpect(jsonPath("$.doctorId").value(2))
                .andExpect(jsonPath("$.doctorName").value("Dr. Kavya Rao"));
    }

    @Test
    void weeklyRosterEndpointReturnsAssignedDoctorTimetable() throws Exception {
        Doctor doctor = new Doctor();
        doctor.setDoctorId(2L);

        Nurse nurse = new Nurse();
        nurse.setNurseId(3L);
        nurse.setDoctor(doctor);

        WeeklyRosterRow row = new WeeklyRosterRow(
                "2026-04-14",
                "2026-04-20",
                "2026-04-15",
                2L,
                "Dr. Kavya Rao",
                "REGULAR",
                4L,
                "09:00",
                "17:00",
                3L,
                "Sneha Iyer"
        );

        when(nurseRepository.findById(3L)).thenReturn(Optional.of(nurse));
        when(rosterService.getWeeklyRosterViewForDoctor(java.time.LocalDate.parse("2026-04-14"), 2L))
                .thenReturn(List.of(row));

        mockMvc.perform(get("/nurses/3/weekly-roster").param("startDate", "2026-04-14"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].doctorName").value("Dr. Kavya Rao"))
                .andExpect(jsonPath("$[0].roomId").value(4));
    }

    @Test
    void assignEndpointRequiresHeadNurseSession() throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("role", "HEAD_NURSE");

        Nurse nurse = new Nurse();
        nurse.setNurseId(3L);
        nurse.setName("Sneha Iyer");

        Doctor doctor = new Doctor();
        doctor.setDoctorId(2L);
        doctor.setName("Dr. Kavya Rao");

        when(nurseRepository.findById(3L)).thenReturn(Optional.of(nurse));
        when(doctorRepository.findById(2L)).thenReturn(Optional.of(doctor));
        when(nurseRepository.save(nurse)).thenReturn(nurse);

        mockMvc.perform(put("/nurses/assign")
                        .session(session)
                        .param("nurseId", "3")
                        .param("doctorId", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nurseId").value(3));
    }
}
