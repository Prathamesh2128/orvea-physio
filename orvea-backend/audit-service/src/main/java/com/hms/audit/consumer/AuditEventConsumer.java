package com.hms.audit.consumer;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.hms.audit.service.AuditIngestService;
import com.hms.common.event.BaseEvent;
import com.hms.common.event.EventJson;
import com.hms.common.event.EventTopics;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class AuditEventConsumer {

	private final AuditIngestService service;

	@KafkaListener(topics = EventTopics.AUDIT, groupId = "audit-service")
	public void onEvent(String json) {
		service.ingest(EventJson.read(json, BaseEvent.class));
	}
}