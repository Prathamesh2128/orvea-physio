package com.hms.notification.consumer;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.hms.common.event.BaseEvent;
import com.hms.common.event.EventJson;
import com.hms.common.event.EventTopics;
import com.hms.notification.service.NotificationService;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class EventConsumer {

	private final NotificationService service;

	/**
	 * A failure here is retried 3 times and then moved to <topic>.DLT by the error
	 * handler.
	 */
	@KafkaListener(topics = { EventTopics.APPOINTMENT, EventTopics.NOTIFICATION }, groupId = "notification-service")
	public void onEvent(String json) {
		service.handle(EventJson.read(json, BaseEvent.class));
	}
}
