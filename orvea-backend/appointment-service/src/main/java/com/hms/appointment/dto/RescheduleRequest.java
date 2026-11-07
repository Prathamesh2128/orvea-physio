package com.hms.appointment.dto;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RescheduleRequest(@NotNull LocalDate date, @NotNull LocalTime startTime, UUID providerId,
		@Size(max = 500) String note) {
}