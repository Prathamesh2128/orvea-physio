package com.hms.appointment.dto;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

public record NextVisit(UUID patientId, UUID appointmentId, UUID providerId, String providerName, LocalDate date,
		LocalTime startTime) {
}