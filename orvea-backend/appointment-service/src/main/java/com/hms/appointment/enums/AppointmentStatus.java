package com.hms.appointment.enums;

import java.util.List;

public enum AppointmentStatus {
	BOOKED, CONFIRMED, CHECKED_IN, IN_PROGRESS, COMPLETED, CANCELLED, NO_SHOW, RESCHEDULED;

	/** Statuses that occupy the provider's time. */
	public static final List<AppointmentStatus> BLOCKING = List.of(BOOKED, CONFIRMED, CHECKED_IN, IN_PROGRESS,
			COMPLETED);

	public boolean blocksSlot() {
		return BLOCKING.contains(this);
	}

	public boolean canMoveTo(AppointmentStatus t) {
		return switch (this) {
		case BOOKED -> t == CONFIRMED || t == CHECKED_IN || t == CANCELLED || t == NO_SHOW || t == RESCHEDULED;
		case CONFIRMED -> t == CHECKED_IN || t == CANCELLED || t == NO_SHOW || t == RESCHEDULED;
		case CHECKED_IN -> t == IN_PROGRESS || t == CANCELLED;
		case IN_PROGRESS -> t == COMPLETED;
		case COMPLETED, CANCELLED, NO_SHOW, RESCHEDULED -> false;
		};
	}
}
