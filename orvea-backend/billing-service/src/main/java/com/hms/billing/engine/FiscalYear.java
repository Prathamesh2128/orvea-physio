package com.hms.billing.engine;

import java.time.LocalDate;

public final class FiscalYear {
	private FiscalYear() {
	}

	/**
	 * Indian financial year runs April to March: 8 Nov 2026 -> "2026-27", 10 Feb
	 * 2027 -> "2026-27". Month 1 = calendar year.
	 */
	public static String label(LocalDate date, int startMonth) {
		if (startMonth <= 1)
			return String.valueOf(date.getYear());
		int startYear = date.getMonthValue() >= startMonth ? date.getYear() : date.getYear() - 1;
		return startYear + "-" + String.format("%02d", (startYear + 1) % 100);
	}

	public static String number(String prefix, String fiscalYear, long sequence) {
		return String.format("%s/%s/%06d", prefix, fiscalYear, sequence);
	}
}