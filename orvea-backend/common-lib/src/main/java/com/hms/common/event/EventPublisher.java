package com.hms.common.event;

import java.util.Map;
import java.util.UUID;

public interface EventPublisher {
	/**
	 * Fire-and-forget; when a transaction is active it is sent only after COMMIT.
	 */
	void publish(String topic, String key, String eventType, UUID clinicId, Map<String, Object> payload);

	/**
	 * Sends immediately and waits for the broker; throws if it cannot be delivered.
	 */
	void publishNow(String topic, String key, String eventType, UUID clinicId, Map<String, Object> payload);

}
