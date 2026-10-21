package com.hms.patient.dto;

import com.hms.patient.enums.CaseStatus;

import jakarta.validation.constraints.NotNull;

public record CaseStatusRequest(@NotNull CaseStatus status, String reason) {

}
