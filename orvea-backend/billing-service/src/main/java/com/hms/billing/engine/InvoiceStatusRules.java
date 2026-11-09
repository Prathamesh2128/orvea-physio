package com.hms.billing.engine;

import java.math.BigDecimal;

import com.hms.billing.enums.InvoiceStatus;

public final class InvoiceStatusRules {
	private InvoiceStatusRules() {
	}

	/**
	 * Status of an issued invoice from its total and the net money received
	 * (payments minus refunds).
	 */
	public static InvoiceStatus forAmounts(BigDecimal grandTotal, BigDecimal netPaid) {
		if (grandTotal.signum() == 0)
			return InvoiceStatus.PAID;
		if (netPaid.compareTo(grandTotal) >= 0)
			return InvoiceStatus.PAID;
		if (netPaid.signum() <= 0)
			return InvoiceStatus.ISSUED;
		return InvoiceStatus.PARTIAL;
	}
}