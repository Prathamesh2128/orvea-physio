package com.hms.appointment.dto;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.UUID;

public record WorkingHoursResponse(UUID id, DayOfWeek dayOfWeek, LocalTime startTime, LocalTime endTime,
		LocalTime lunchStart, LocalTime lunchEnd) {
}
