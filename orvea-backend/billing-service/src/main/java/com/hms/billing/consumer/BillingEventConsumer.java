package com.hms.billing.consumer;

import com.hms.billing.service.ChargeService;
import com.hms.common.event.BaseEvent;
import com.hms.common.event.EventJson;
import com.hms.common.event.EventTopics;
import com.hms.common.event.EventTypes;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class BillingEventConsumer {

	private final ChargeService chargeService;

	/**
	 * Failures are retried 3 times, then moved to <topic>.DLT by the error handler.
	 */
	@KafkaListener(topics = EventTopics.APPOINTMENT, groupId = "billing-service")
	public void onEvent(String json) {
		BaseEvent e = EventJson.read(json, BaseEvent.class);
		if (EventTypes.APPOINTMENT_COMPLETED.equals(e.getEventType()))
			chargeService.onVisitCompleted(e);
	}
}