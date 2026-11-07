package com.hms.staff.utill;

import com.hms.common.enums.RoleType;

public class DisplayNameFormatter {
	private DisplayNameFormatter() {
	}

	public static String format(RoleType type, String first, String middle, String last) {
		String name = String.join(" ", clean(first), clean(middle), clean(last)).trim().replaceAll("\\s+", " ");
		name = name.replaceFirst("(?i)^dr(\\.\\s*|\\s+)", "") // typed "Dr." / "Dr " prefix ("Drake" is safe)
				.replaceFirst("(?i)\\s*\\(pt\\)\\s*$", "") // typed "(PT)" suffix
				.trim();
		return switch (type) {
		case PHYSIOTHERAPIST -> "Dr. " + name + "(PT)";
		case DOCTOR -> "Dr. " + name;
		default -> name;
		};
	}

	private static String clean(String s) {
		return s == null ? "" : s.trim();
	}
}
