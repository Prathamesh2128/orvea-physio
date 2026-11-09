package com.hms.notification.util;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public final class WaLink {
	private WaLink() {
	}

	/** Opens WhatsApp (app or web) with the chat and the message already typed. */
	public static String build(String phoneDigits, String text) {
		String encoded = URLEncoder.encode(text, StandardCharsets.UTF_8).replace("+", "%20");
		return "https://wa.me/" + phoneDigits + "?text=" + encoded;
	}
}