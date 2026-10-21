package com.hms.patient.dto;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import com.hms.common.enums.BodyRegion;
import com.hms.common.enums.Laterality;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

// Create and update. 'conditions' is only read on create (use the condition endpoints afterwards).
public record CaseRequest(
        @NotBlank @Size(max = 255) String diagnosis,
        @Size(max = 20) String icdCode,
        @NotNull BodyRegion bodyRegion,
        Laterality laterality,
        UUID primaryTherapistId,
        @Min(1) @Max(500) int plannedSessions,
        LocalDate startDate,
        LocalDate expectedEndDate,
        @Size(max = 1000) String notes,
        @Valid List<ConditionRequest> conditions) {}
