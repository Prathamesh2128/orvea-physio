package com.hms.assessment.dto;

import java.time.LocalDate;
import java.util.UUID;

import com.hms.assessment.enums.AssessmentType;

import jakarta.validation.constraints.NotNull;

public record CreateAssessmentRequest(@NotNull UUID patientId, @NotNull UUID caseId, AssessmentType type,
		LocalDate visitDate, UUID appointmentId, Boolean copyFromLast) {
}