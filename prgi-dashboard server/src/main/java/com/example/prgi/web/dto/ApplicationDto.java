package com.example.prgi.web.dto;

import com.example.prgi.domain.ModuleType;
import com.example.prgi.domain.Status;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record ApplicationDto(
        Long id,
        ModuleType moduleType,
        Status status,
        String applicantName,
        String referenceNo,
        LocalDate submittedDate,
        LocalDateTime lastUpdated,
        String remarks
) {}