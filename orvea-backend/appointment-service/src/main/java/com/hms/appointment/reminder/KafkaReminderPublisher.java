package com.hms.appointment.reminder;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.hms.common.event.EventPublisher;
import com.hms.common.event.EventTopics;
import com.hms.common.event.EventTypes;

import lombok.RequiredArgsConstructor;

/**
 * Hands a due reminder to notification-service. Blocks until Kafka accepts it,
 * so failures are retried.
 */
@Component
@RequiredArgsConstructor
public class KafkaReminderPublisher implements ReminderPublisher {

	private final EventPublisher events;

	@Override
	public void publish(ReminderMessage m) {
		Map<String, Object> p = new LinkedHashMap<>();
		p.put("taskId", m.taskId().toString());
		p.put("appointmentId", m.appointmentId().toString());
		p.put("patientId", m.patientId().toString());
		p.put("patientName", m.patientName());
		p.put("patientPhone", m.patientPhone());
		p.put("providerName", m.providerName());
		p.put("visitTypeName", m.visitTypeName());
		p.put("appointmentAt", m.appointmentAt().toString());
		p.put("minutesBefore", m.minutesBefore());
		events.publishNow(EventTopics.NOTIFICATION, m.appointmentId().toString(), EventTypes.REMINDER_DUE, m.clinicId(),
				p);
	}
}