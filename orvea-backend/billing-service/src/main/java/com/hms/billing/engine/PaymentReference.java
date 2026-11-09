package com.hms.billing.engine;

/**
 * Card numbers must never be stored. A reference that looks like one is
 * refused.
 */
public final class PaymentReference {
	private PaymentReference() {
	}

	public static boolean looksLikeCardNumber(String reference) {
		if (reference == null)
			return false;
		return reference.replaceAll("[\\s-]", "").matches("\\d{13,19}");
	}
}