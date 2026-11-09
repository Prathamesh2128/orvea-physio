package com.hms.billing.engine;

public final class Aging {
	private Aging() {
	}

	/** 0 = up to 30 days, 1 = 31-60, 2 = 61-90, 3 = over 90. */
	public static int bucket(long days) {
		if (days <= 30)
			return 0;
		if (days <= 60)
			return 1;
		if (days <= 90)
			return 2;
		return 3;
	}
}