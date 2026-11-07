package com.hms.appointment.dto;

import java.time.LocalDateTime;
import java.util.UUID;

import com.hms.appointment.enums.ReminderStatus;

public record ReminderResponse(UUID id, LocalDateTime remindAt, int offsetMinutes, String channels,
		ReminderStatus status, LocalDateTime dispatchedAt, String lastError) {
}