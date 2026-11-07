package com.hms.appointment.dto;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

public record AvailabilityResponse(UUID providerId, LocalDate date, UUID visitTypeId, int durationMinutes,
		List<LocalTime> starts) {
}
