package com.hms.common.audit;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import com.hms.common.event.EventPublisher;
import com.hms.common.event.EventTopics;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class EventBackedAuditPublisher implements AuditPublisher {

	private final EventPublisher events;

	@Override
	public void recordAs(String actor, UUID clinicId, String action, String entityType, Object entityId,
			Map<String, Object> details) {
		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("actor", actor);
		payload.put("entityType", entityType);
		payload.put("entityId", String.valueOf(entityId));
		payload.put("details", details == null ? Map.of() : details);
		events.publish(EventTopics.AUDIT, String.valueOf(entityId), action, clinicId, payload);
	}
}
