package com.hms.patient.dto;

import java.time.LocalDateTime;
import java.util.UUID;

import com.hms.patient.enums.ConsentGivenBy;
import com.hms.patient.enums.ConsentStatus;
import com.hms.patient.enums.ConsentType;

public record ConsentResponse(UUID id, ConsentType consentType, ConsentStatus status, ConsentGivenBy givenBy,
		String signerName, String consentVersion, String documentRef, LocalDateTime grantedAt,
		LocalDateTime withdrawnAt, String withdrawalReason, String notes) { }
