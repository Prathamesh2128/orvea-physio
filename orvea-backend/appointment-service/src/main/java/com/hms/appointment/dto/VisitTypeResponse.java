package com.hms.appointment.dto;

import java.util.UUID;

public record VisitTypeResponse(UUID id, String code, String name, int durationMinutes, boolean countsAsSession,
		boolean active, int sortOrder) {
}
