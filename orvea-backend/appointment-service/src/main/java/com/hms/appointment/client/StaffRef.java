package com.hms.appointment.client;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record StaffRef(UUID id, String displayName, String staffType, boolean active, boolean bookable, UUID clinicId) {
}
