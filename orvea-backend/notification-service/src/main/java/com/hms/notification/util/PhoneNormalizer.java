package com.hms.notification.util;

public final class PhoneNormalizer {
	private PhoneNormalizer() {
	}

	/**
	 * @return digits with country code (e.g. 919876543210), or null when the number
	 *         cannot be used.
	 */
	public static String normalize(String raw, String defaultCountryCode) {
		if (raw == null)
			return null;
		String d = raw.replaceAll("\\D", "");
		if (d.isEmpty())
			return null;
		String cc = defaultCountryCode == null || defaultCountryCode.isBlank() ? "91"
				: defaultCountryCode.replaceAll("\\D", "");
		if (d.length() == 10)
			return cc + d;
		if (d.length() == 11 && d.startsWith("0"))
			return cc + d.substring(1);
		if (d.length() >= 11 && d.length() <= 15)
			return d;
		return null;
	}
}
