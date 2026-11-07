package com.hms.patient.dto;

import java.util.UUID;

public record PatientRef(UUID id, String uhid, String fullName, String phone, String countryCode, boolean active,
		UUID clinicId) {
}
