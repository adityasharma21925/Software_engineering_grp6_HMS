package com.gray.hospital.controller;

import com.gray.hospital.entity.Doctor;
import com.gray.hospital.repository.DoctorRepository;
import com.gray.hospital.repository.NurseRepository;
import com.gray.hospital.repository.PatientRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
class AuthControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PatientRepository patientRepository;

    @MockBean
    private DoctorRepository doctorRepository;

    @MockBean
    private NurseRepository nurseRepository;

    @Test
    void loginEndpointReturnsDoctorRoleAndUserId() throws Exception {
        Doctor doctor = new Doctor();
        doctor.setDoctorId(5L);
        doctor.setEmail("doctor@gray.com");
        doctor.setPassword("doc123");

        when(patientRepository.findByEmail("doctor@gray.com")).thenReturn(Optional.empty());
        when(doctorRepository.findByEmail("doctor@gray.com")).thenReturn(Optional.of(doctor));

        mockMvc.perform(post("/auth/login")
                        .param("email", "doctor@gray.com")
                        .param("password", "doc123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("DOCTOR"))
                .andExpect(jsonPath("$.userId").value(5));
    }

}
