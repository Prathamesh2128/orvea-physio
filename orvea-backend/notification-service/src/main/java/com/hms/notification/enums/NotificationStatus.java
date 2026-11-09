package com.hms.notification.enums;

import java.util.List;

public enum NotificationStatus {
	PENDING, // waiting for automatic sending (CLOUD_API)
	SENDING, READY_TO_SEND, // in the outbox; staff taps the WhatsApp link (MANUAL_LINK)
	SENT, LOGGED, // LOG_ONLY provider
	FAILED, SUPPRESSED, // intentionally not sent; statusReason says why
	CANCELLED;

	/**
	 * Not yet delivered, so they can still be withdrawn when the appointment
	 * changes.
	 */
	public static final List<NotificationStatus> OPEN = List.of(PENDING, READY_TO_SEND);
}
