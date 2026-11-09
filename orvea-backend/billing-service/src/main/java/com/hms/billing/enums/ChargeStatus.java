package com.hms.billing.enums;

public enum ChargeStatus {
	PENDING, // delivered, waiting to be put on an invoice
	COVERED_BY_PACKAGE, // a package session was used
	INVOICED, WAIVED, // free, with a reason
	NO_CHARGE, // the service has no fee
	UNMAPPED // no service is linked to this visit type yet: needs attention
}
