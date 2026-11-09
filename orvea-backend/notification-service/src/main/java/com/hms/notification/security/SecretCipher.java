package com.hms.notification.security;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** AES-256-GCM for secrets stored in the database (WhatsApp access tokens). */
@Component
public class SecretCipher {

	private final SecretKeySpec key;
	private final SecureRandom random = new SecureRandom();

	public SecretCipher(@Value("${physio.notification.secret-key:}") String base64Key) {
		String k = base64Key == null ? "" : base64Key.trim();
		if (k.isEmpty()) {
			throw new IllegalStateException("physio.notification.secret-key is not set. Generate 32 random bytes "
					+ "(Base64) and set it in application.yml or the NOTIFICATION_SECRET_KEY environment variable.");
		}
		byte[] bytes;
		try {
			bytes = Base64.getDecoder().decode(k);
		} catch (IllegalArgumentException e) {
			throw new IllegalStateException("physio.notification.secret-key is not valid Base64", e);
		}
		if (bytes.length != 32) {
			throw new IllegalStateException(
					"physio.notification.secret-key must decode to exactly 32 bytes, got " + bytes.length);
		}
		this.key = new SecretKeySpec(bytes, "AES");
	}

	public String encrypt(String plain) {
		try {
			byte[] iv = new byte[12];
			random.nextBytes(iv);
			Cipher c = Cipher.getInstance("AES/GCM/NoPadding");
			c.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(128, iv));
			byte[] ct = c.doFinal(plain.getBytes(StandardCharsets.UTF_8));
			return Base64.getEncoder().encodeToString(iv) + ":" + Base64.getEncoder().encodeToString(ct);
		} catch (GeneralSecurityException e) {
			throw new IllegalStateException("Encryption failed", e);
		}
	}

	public String decrypt(String stored) {
		try {
			String[] parts = stored.split(":", 2);
			Cipher c = Cipher.getInstance("AES/GCM/NoPadding");
			c.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(128, Base64.getDecoder().decode(parts[0])));
			return new String(c.doFinal(Base64.getDecoder().decode(parts[1])), StandardCharsets.UTF_8);
		} catch (GeneralSecurityException | RuntimeException e) {
			throw new IllegalStateException("Could not decrypt the stored secret", e);
		}
	}
}