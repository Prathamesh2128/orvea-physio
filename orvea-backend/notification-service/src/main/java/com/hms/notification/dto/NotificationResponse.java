package com.hms.notification.dto;

import java.time.LocalDateTime;
import java.util.UUID;

import com.hms.notification.enums.MessageType;
import com.hms.notification.enums.NotificationStatus;

public record NotificationResponse(UUID id, MessageType messageType, String channel, String provider,
		String recipientName, String recipientPhone, String body, NotificationStatus status, String statusReason,
		String waLink, UUID appointmentId, LocalDateTime appointmentAt, int attempts, String lastError,
		LocalDateTime createdAt, LocalDateTime sentAt, String sentBy) {
}
