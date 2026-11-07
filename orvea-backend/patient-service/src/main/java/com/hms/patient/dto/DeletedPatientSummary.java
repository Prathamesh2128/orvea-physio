package com.hms.patient.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record DeletedPatientSummary(UUID id, String uhid, String fullName, String phone, LocalDateTime deletedAt,
		String deletedBy) {
}