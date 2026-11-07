package com.hms.appointment.reminder;

/**
 * Phase 5 replaces the logging implementation with a Kafka producer to
 * notification-service.
 */
public interface ReminderPublisher {
	void publish(ReminderMessage message);
}
