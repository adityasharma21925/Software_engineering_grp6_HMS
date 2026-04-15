package com.gray.hospital.controller.dto;

public record NurseViewRow(
        Long nurseId,
        String name,
        String email,
        String phone,
        String role,
        String salary,
        Long doctorId,
        String doctorName
) {
}
