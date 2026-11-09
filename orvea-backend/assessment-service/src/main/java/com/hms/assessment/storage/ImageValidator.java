package com.hms.assessment.storage;

import java.util.Arrays;

/**
 * Decides the file type from the first bytes, not from the name or the
 * Content-Type the client claims.
 */
public final class ImageValidator {
	private ImageValidator() { }

	public record Detected(String extension, String contentType) { }

	public static Detected detect(byte[] d) {
		if (d == null || d.length < 12)
			return null;
		if ((d[0] & 0xFF) == 0xFF && (d[1] & 0xFF) == 0xD8 && (d[2] & 0xFF) == 0xFF)
			return new Detected("jpg", "image/jpeg");
		byte[] png = { (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A };
		if (Arrays.equals(Arrays.copyOfRange(d, 0, 8), png))
			return new Detected("png", "image/png");
		if (new String(d, 0, 4, java.nio.charset.StandardCharsets.US_ASCII).equals("RIFF")
				&& new String(d, 8, 4, java.nio.charset.StandardCharsets.US_ASCII).equals("WEBP")) {
			return new Detected("webp", "image/webp");
		}
		return null;
	}
}