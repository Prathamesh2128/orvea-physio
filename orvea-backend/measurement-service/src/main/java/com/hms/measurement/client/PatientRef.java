package com.hms.measurement.client;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record PatientRef(UUID id, String uhid, String fullName, String phone, String countryCode, boolean active,
		UUID clinicId) {
}
