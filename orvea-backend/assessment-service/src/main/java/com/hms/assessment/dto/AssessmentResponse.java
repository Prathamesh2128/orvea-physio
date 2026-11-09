package com.hms.assessment.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.hms.assessment.engine.FormEngine;
import com.hms.assessment.enums.AssessmentStatus;
import com.hms.assessment.enums.AssessmentType;

public record AssessmentResponse(UUID id, UUID patientId, String patientUhid, UUID caseId, String caseNumber,
		AssessmentType type, LocalDate visitDate, AssessmentStatus status, Long version, Map<String, Object> form,
		Map<String, Map<String, Object>> answers, List<String> carriedForwardKeys, UUID copiedFromId,
		FormEngine.Progress progress, long unconfirmedCaptures, List<AddendumResponse> addenda, LocalDateTime createdAt,
		LocalDateTime completedAt, String completedBy) {
}