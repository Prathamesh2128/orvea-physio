package com.hms.patient.dto;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import com.hms.common.enums.BodyRegion;
import com.hms.common.enums.Laterality;
import com.hms.patient.enums.CaseStatus;

public record CaseResponse(UUID id, UUID patientId, String caseNumber, String diagnosis, String icdCode,
		BodyRegion bodyRegion, Laterality laterality, UUID primaryTherapistId, int plannedSessions,
		int completedSessions, CaseStatus status, LocalDate startDate, LocalDate expectedEndDate, LocalDate closedOn,
		String closureReason, String notes, List<ConditionResponse> conditions) {

}
