package com.hms.notification.enums;

public enum MessageType {
	BOOKING_CONFIRMATION("Booking confirmation"), REMINDER("Reminder"), CANCELLATION("Cancellation"),
	RESCHEDULE("Reschedule"), MEASUREMENT_REMINDER("Measurement reminder");

	private final String label;

	MessageType(String label) {
		this.label = label;
	}

	public String label() {
		return label;
	}
}
