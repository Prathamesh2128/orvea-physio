package com.hms.assessment.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import com.hms.assessment.enums.AssessmentStatus;
import com.hms.assessment.enums.AssessmentType;

public record AssessmentSummary(UUID id, UUID caseId, String caseNumber, AssessmentType type, LocalDate visitDate,
		AssessmentStatus status, LocalDateTime completedAt, String completedBy) {
}