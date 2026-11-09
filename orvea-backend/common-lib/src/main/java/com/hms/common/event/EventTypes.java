package com.hms.common.event;

public final class EventTypes {
	private EventTypes() {
	}

	public static final String APPOINTMENT_BOOKED = "APPOINTMENT_BOOKED";
	public static final String APPOINTMENT_RESCHEDULED = "APPOINTMENT_RESCHEDULED";
	public static final String APPOINTMENT_CANCELLED = "APPOINTMENT_CANCELLED";
	public static final String REMINDER_DUE = "REMINDER_DUE";
	public static final String MEASUREMENT_DUE = "MEASUREMENT_DUE";
	public static final String APPOINTMENT_COMPLETED = "APPOINTMENT_COMPLETED";
}
