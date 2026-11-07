package com.hms.appointment.dto;

import java.util.UUID;

public record ProviderSettingsResponse(UUID providerId, String providerName, int slotMinutes, int maxParallel,
		boolean active) {
}
