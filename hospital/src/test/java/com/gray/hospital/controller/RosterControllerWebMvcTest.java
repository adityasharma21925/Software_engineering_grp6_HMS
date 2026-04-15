package com.gray.hospital.controller;

import com.gray.hospital.controller.dto.PublishedRosterSummary;
import com.gray.hospital.controller.dto.RosterComplianceStatus;
import com.gray.hospital.controller.dto.WeeklyRosterRow;
import com.gray.hospital.entity.Doctor;
import com.gray.hospital.entity.Roster;
import com.gray.hospital.service.RosterService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(RosterController.class)
class RosterControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private RosterService rosterService;

    @Test
    void weekViewEndpointReturnsRosterRows() throws Exception {
        WeeklyRosterRow row = new WeeklyRosterRow(
                "2026-04-14",
                "2026-04-20",
                "2026-04-15",
                1L,
                "Dr. Arjun Mehta",
                "REGULAR",
                2L,
                "09:00",
                "17:00",
                1L,
                "Priya Das"
        );

        when(rosterService.getWeeklyRosterView(LocalDate.parse("2026-04-14"))).thenReturn(List.of(row));

        mockMvc.perform(get("/roster/week-view").param("startDate", "2026-04-14"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].doctorName").value("Dr. Arjun Mehta"))
                .andExpect(jsonPath("$[0].nurseName").value("Priya Das"));
    }

    @Test
    void weekComplianceEndpointReturnsValidationSummary() throws Exception {
        RosterComplianceStatus status = new RosterComplianceStatus(
                "2026-04-14",
                "2026-04-20",
                true,
                false,
                true,
                List.of("2026-04-15: requires at least 1 regular doctor during 20:00-08:00, found 0")
        );

        when(rosterService.getWeeklyCompliance(LocalDate.parse("2026-04-14"))).thenReturn(status);

        mockMvc.perform(get("/roster/week-compliance").param("startDate", "2026-04-14"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dayCoverageMet").value(true))
                .andExpect(jsonPath("$.nightCoverageMet").value(false))
                .andExpect(jsonPath("$.issues[0]").exists());
    }

    @Test
    void publishWeekEndpointReturnsPublicationSummary() throws Exception {
        PublishedRosterSummary summary = new PublishedRosterSummary(
                "2026-04-14",
                "2026-04-20",
                "2026-04-15T13:00:00",
                "target/roster-publications/week-2026-04-14.json",
                4,
                List.of("Email queued for Dr. Arjun Mehta for 2026-04-15 09:00-17:00 in Room 1")
        );

        when(rosterService.publishWeek(eq(LocalDate.parse("2026-04-14")))).thenReturn(summary);

        mockMvc.perform(post("/roster/publish-week").param("date", "2026-04-14"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rosterCount").value(4))
                .andExpect(jsonPath("$.doctorNotifications[0]").exists());
    }

    @Test
    void addEndpointReturnsCreatedRoster() throws Exception {
        Doctor doctor = new Doctor();
        doctor.setDoctorId(1L);
        doctor.setName("Dr. Arjun Mehta");

        Roster roster = new Roster();
        roster.setDoctor(doctor);
        roster.setRoomId(1L);
        roster.setDate(LocalDate.parse("2026-04-15"));
        roster.setStartTime(LocalTime.of(9, 0));
        roster.setEndTime(LocalTime.of(17, 0));

        when(rosterService.addRoster(1L, 1L, LocalDate.parse("2026-04-15"), "09:00", "17:00")).thenReturn(roster);

        mockMvc.perform(post("/roster/add")
                        .param("doctorId", "1")
                        .param("roomId", "1")
                        .param("date", "2026-04-15")
                        .param("start", "09:00")
                        .param("end", "17:00"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.doctor.name").value("Dr. Arjun Mehta"))
                .andExpect(jsonPath("$.roomId").value(1));
    }
}
