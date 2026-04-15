package com.gray.hospital.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gray.hospital.controller.dto.PublishedRosterSummary;
import com.gray.hospital.controller.dto.WeeklyRosterRow;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class RosterPublicationService {

    private final ObjectMapper objectMapper;

    public RosterPublicationService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public PublishedRosterSummary publishWeek(LocalDate weekStart, List<WeeklyRosterRow> rows) {
        try {
            Path publishDir = Path.of("target", "roster-publications");
            Files.createDirectories(publishDir);

            LocalDate weekEnd = weekStart.plusDays(6);
            LocalDateTime publishedAt = LocalDateTime.now();

            Path publicationFile = publishDir.resolve("week-" + weekStart + ".json");
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(publicationFile.toFile(), rows);

            List<String> notifications = new ArrayList<>();
            for (WeeklyRosterRow row : rows) {
                String doctorEmailStatus = "Email queued for " + row.doctorName() + " for " + row.date()
                        + " " + row.startTime() + "-" + row.endTime() + " in Room " + row.roomId();
                notifications.add(doctorEmailStatus);
            }

            Path notificationsFile = publishDir.resolve("doctor-notifications.log");
            if (!notifications.isEmpty()) {
                Files.write(
                        notificationsFile,
                        notifications.stream()
                                .map(line -> publishedAt + " | " + line + System.lineSeparator())
                                .toList(),
                        StandardOpenOption.CREATE,
                        StandardOpenOption.APPEND
                );
            }

            return new PublishedRosterSummary(
                    weekStart.toString(),
                    weekEnd.toString(),
                    publishedAt.toString(),
                    publicationFile.toString(),
                    rows.size(),
                    notifications
            );
        } catch (IOException exception) {
            throw new RuntimeException("Failed to publish weekly roster", exception);
        }
    }
}
