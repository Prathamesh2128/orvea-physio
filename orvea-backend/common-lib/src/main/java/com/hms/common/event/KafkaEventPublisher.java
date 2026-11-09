package com.hms.common.event;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.hms.common.security.SecurityUtils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class KafkaEventPublisher implements EventPublisher {

	private final KafkaTemplate<String, String> template;
	private final String applicationName;

	@Override
	public void publish(String topic, String key, String eventType, UUID clinicId, Map<String, Object> payload) {
		String json = build(eventType, clinicId, payload);
		Runnable send = () -> template.send(topic, key, json).whenComplete((r, ex) -> {
			if (ex != null)
				log.error("Kafka publish failed topic={} type={} key={}", topic, eventType, key, ex);
		});
		if (TransactionSynchronizationManager.isSynchronizationActive()) {
			TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
				@Override
				public void afterCommit() {
					send.run();
				}
			});
		} else {
			send.run();
		}
	}

	@Override
	public void publishNow(String topic, String key, String eventType, UUID clinicId, Map<String, Object> payload) {
		String json = build(eventType, clinicId, payload);
		try {
			template.send(topic, key, json).get(10, TimeUnit.SECONDS);
		} catch (Exception e) {
			throw new IllegalStateException("Kafka publish failed for " + eventType + ": " + e.getMessage(), e);
		}
	}

	private String build(String eventType, UUID clinicId, Map<String, Object> payload) {
		BaseEvent e = new BaseEvent();
		e.setEventType(eventType);
		e.setSourceService(applicationName);
		e.setClinicId(clinicId);
		e.setPerformedBy(SecurityUtils.currentUsername());
		e.setPayload(payload);
		return EventJson.write(e);
	}
}
