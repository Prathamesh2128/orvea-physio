package com.hms.patient.dto;

import java.util.List;
import java.util.UUID;

public record CaseRef(UUID id, UUID patientId, String caseNumber, String status, String bodyRegion, String laterality,
		List<String> conditionCodes, UUID primaryTherapistId) {
}
