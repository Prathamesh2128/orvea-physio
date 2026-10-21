package com.hms.patient.util;

import com.hms.common.exception.BusinessException;

public final class PhoneUtil {
	//Digits only; when 10+ digits, keeps the last 10 (drops +91 / leading 0). Null/blank -> null.
	public static String normalize(String raw) {
		if (raw == null || raw.isBlank())
			return null;
		String digits = raw.replaceAll("\\D", "");
		if (digits.length() < 7) {
			throw new BusinessException("Invalid phone number: " + raw);
		}
		return digits.length() > 10 ? digits.substring(digits.length() - 10) : digits;
	}
}
