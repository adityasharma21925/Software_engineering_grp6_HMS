package com.gray.hospital.controller.dto;

import java.util.List;

public record RosterComplianceStatus(
        String weekStart,
        String weekEnd,
        boolean dayCoverageMet,
        boolean nightCoverageMet,
        boolean visitingRulesMet,
        List<String> issues
) {
}
