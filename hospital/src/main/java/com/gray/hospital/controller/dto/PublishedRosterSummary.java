package com.gray.hospital.controller.dto;

import java.util.List;

public record PublishedRosterSummary(
        String weekStart,
        String weekEnd,
        String publishedAt,
        String publicationFile,
        int rosterCount,
        List<String> doctorNotifications
) {
}
