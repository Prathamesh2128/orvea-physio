package com.hms.patient.enums;

public enum CaseStatus {
	PLANNED, ACTIVE, ON_HOLD, COMPLETED, CANCELLED;

	public boolean canMoveTo(CaseStatus target) {
		return switch (this) {
		case PLANNED -> target == ACTIVE || target == CANCELLED;
		case ACTIVE -> target == ON_HOLD || target == COMPLETED || target == CANCELLED;
		case ON_HOLD -> target == ACTIVE || target == CANCELLED;
		case COMPLETED, CANCELLED -> false;
		};
	}
}
