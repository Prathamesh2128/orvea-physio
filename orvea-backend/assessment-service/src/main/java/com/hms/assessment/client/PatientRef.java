package com.hms.assessment.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public record PatientRef(UUID id, String uhid, String fullName, String phone, String countryCode, boolean active,
		UUID clinicId) {
}