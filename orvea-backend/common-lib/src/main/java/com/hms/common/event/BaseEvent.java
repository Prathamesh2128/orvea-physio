package com.hms.common.event;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BaseEvent {
	private String eventId = UUID.randomUUID().toString();
	private String eventType;
	private String sourceService;
	private UUID clinicId;
	private String performedBy;
	private Instant occurredAt = Instant.now();
	private Map<String, Object> payload;

	/** Payload value as text, or null. */
	public String text(String key) {
		Object v = payload == null ? null : payload.get(key);
		return v == null ? null : v.toString();
	}

	/** Payload value as UUID, or null when absent or malformed. */
	public UUID uuid(String key) {
		String v = text(key);
		try {
			return v == null || v.isBlank() ? null : UUID.fromString(v);
		} catch (IllegalArgumentException e) {
			return null;
		}
	}
}