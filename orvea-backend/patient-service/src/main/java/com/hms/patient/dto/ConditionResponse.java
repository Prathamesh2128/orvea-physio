package com.hms.patient.dto;

import java.time.LocalDate;
import java.util.UUID;

public record ConditionResponse(UUID id, String conditionCode, String conditionName,
        String notes, LocalDate diagnosedOn, boolean active) {}
