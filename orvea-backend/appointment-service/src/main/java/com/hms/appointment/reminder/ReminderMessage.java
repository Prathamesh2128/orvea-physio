package com.hms.appointment.reminder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import com.hms.appointment.enums.ReminderChannel;

public record ReminderMessage(UUID taskId, UUID appointmentId, UUID clinicId, UUID patientId, String patientName,
		String patientPhone, String providerName, String visitTypeName, LocalDateTime appointmentAt,
		List<ReminderChannel> channelPreference, int minutesBefore) {
}