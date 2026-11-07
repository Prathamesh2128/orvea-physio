package com.hms.appointment.dto;

import java.time.DayOfWeek;
import java.time.LocalTime;

import jakarta.validation.constraints.NotNull;

public record WorkingHoursRequest(@NotNull DayOfWeek dayOfWeek, @NotNull LocalTime startTime,
		@NotNull LocalTime endTime, LocalTime lunchStart, LocalTime lunchEnd) {
}
