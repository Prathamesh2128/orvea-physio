package com.hms.assessment.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public record CaseRef(UUID id, UUID patientId, String caseNumber, String status, String bodyRegion, String laterality,
		List<String> conditionCodes, UUID primaryTherapistId) {
}