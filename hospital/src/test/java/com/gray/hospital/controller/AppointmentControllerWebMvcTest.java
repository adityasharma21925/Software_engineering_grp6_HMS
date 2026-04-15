package com.gray.hospital.controller;

import com.gray.hospital.entity.Doctor;
import com.gray.hospital.repository.AppointmentRepository;
import com.gray.hospital.service.AppointmentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AppointmentController.class)
class AppointmentControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AppointmentService appointmentService;

    @MockBean
    private AppointmentRepository appointmentRepository;

    @Test
    void doctorsForDayEndpointReturnsAvailableDoctors() throws Exception {
        Doctor doctor = new Doctor();
        doctor.setDoctorId(1L);
        doctor.setName("Dr. Arjun Mehta");
        doctor.setDoctorType("REGULAR");

        when(appointmentService.getAvailableDoctors(LocalDate.parse("2026-04-15"))).thenReturn(List.of(doctor));

        mockMvc.perform(get("/appointments/doctors/day").param("date", "2026-04-15"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Dr. Arjun Mehta"))
                .andExpect(jsonPath("$[0].doctorType").value("REGULAR"));
    }

    @Test
    void slotsEndpointReturnsAvailableTimeWindows() throws Exception {
        when(appointmentService.getAvailableSlots(1L, LocalDate.parse("2026-04-15"), 60))
                .thenReturn(List.of(Map.of(
                        "start", LocalDateTime.parse("2026-04-15T09:00:00"),
                        "end", LocalDateTime.parse("2026-04-15T10:00:00")
                )));

        mockMvc.perform(get("/appointments/slots")
                        .param("doctorId", "1")
                        .param("date", "2026-04-15")
                        .param("slotMinutes", "60"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].start").value("2026-04-15T09:00:00"))
                .andExpect(jsonPath("$[0].end").value("2026-04-15T10:00:00"));
    }

    @Test
    void feesEndpointReturnsBookingFee() throws Exception {
        mockMvc.perform(get("/appointments/fees"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.appointmentBookingFee").value(500));
    }
}
