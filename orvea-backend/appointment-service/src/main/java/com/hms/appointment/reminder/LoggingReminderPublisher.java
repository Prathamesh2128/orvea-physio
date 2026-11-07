package com.hms.appointment.reminder;

import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

/**
 * Placeholder: logs what WOULD be sent. No SMS or WhatsApp leaves the system in
 * Phase 4.
 */
@Slf4j
@Component
public class LoggingReminderPublisher implements ReminderPublisher {

	@Override
	public void publish(ReminderMessage m) {
		String phone = m.patientPhone() == null || m.patientPhone().length() < 4 ? "****"
				: "******" + m.patientPhone().substring(m.patientPhone().length() - 4);
		log.info("[REMINDER-STUB] {} min before | {} | patient={} phone={} | with {} | channels={}", m.minutesBefore(),
				m.appointmentAt(), m.patientName(), phone, m.providerName(), m.channelPreference());
	}
}
