package com.hms.measurement.client;

import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record CaseRef(UUID id, UUID patientId, String caseNumber, String status, String bodyRegion, String laterality,
		List<String> conditionCodes, UUID primaryTherapistId) {
}
