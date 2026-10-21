package com.hms.patient.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ConditionRequest(@NotBlank @Size(max = 50) String conditionCode,
        @Size(max = 500) String notes, LocalDate diagnosedOn) {}