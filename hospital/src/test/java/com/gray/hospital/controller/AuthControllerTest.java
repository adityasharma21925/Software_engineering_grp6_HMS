package com.gray.hospital.controller;

import com.gray.hospital.entity.Doctor;
import com.gray.hospital.entity.Nurse;
import com.gray.hospital.repository.DoctorRepository;
import com.gray.hospital.repository.NurseRepository;
import com.gray.hospital.repository.PatientRepository;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private PatientRepository patientRepository;

    @Mock
    private DoctorRepository doctorRepository;

    @Mock
    private NurseRepository nurseRepository;

    @InjectMocks
    private AuthController authController;

    @Test
    void loginReturnsDoctorRoleAndUserId() {
        Doctor doctor = new Doctor();
        doctor.setDoctorId(7L);
        doctor.setEmail("doctor@gray.com");
        doctor.setPassword("doc123");

        when(patientRepository.findByEmail("doctor@gray.com")).thenReturn(Optional.empty());
        when(doctorRepository.findByEmail("doctor@gray.com")).thenReturn(Optional.of(doctor));

        MockHttpServletRequest request = new MockHttpServletRequest();

        Map<String, Object> response = authController.login("doctor@gray.com", "doc123", request);

        assertEquals("DOCTOR", response.get("role"));
        assertEquals(7L, response.get("userId"));
        HttpSession session = request.getSession(false);
        assertEquals("DOCTOR", session.getAttribute("role"));
        assertEquals(7L, session.getAttribute("userId"));
    }

    @Test
    void loginMapsHeadNurseRoleToHeadNurseSessionValue() {
        Nurse nurse = new Nurse();
        nurse.setNurseId(3L);
        nurse.setEmail("head@gray.com");
        nurse.setPassword("nurse123");
        nurse.setRole("HEAD");

        when(patientRepository.findByEmail("head@gray.com")).thenReturn(Optional.empty());
        when(doctorRepository.findByEmail("head@gray.com")).thenReturn(Optional.empty());
        when(nurseRepository.findByEmail("head@gray.com")).thenReturn(Optional.of(nurse));

        MockHttpServletRequest request = new MockHttpServletRequest();

        Map<String, Object> response = authController.login("head@gray.com", "nurse123", request);

        assertEquals("HEAD_NURSE", response.get("role"));
        assertEquals(3L, response.get("userId"));
        assertEquals("HEAD_NURSE", request.getSession(false).getAttribute("role"));
    }

    @Test
    void loginThrowsForInvalidCredentials() {
        when(patientRepository.findByEmail("unknown@gray.com")).thenReturn(Optional.empty());
        when(doctorRepository.findByEmail("unknown@gray.com")).thenReturn(Optional.empty());
        when(nurseRepository.findByEmail("unknown@gray.com")).thenReturn(Optional.empty());

        MockHttpServletRequest request = new MockHttpServletRequest();

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> authController.login("unknown@gray.com", "bad", request));

        assertEquals("Invalid credentials", exception.getMessage());
    }
}
