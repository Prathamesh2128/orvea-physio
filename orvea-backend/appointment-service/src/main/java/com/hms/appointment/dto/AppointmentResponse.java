package com.hms.appointment.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

import com.hms.appointment.enums.AppointmentStatus;
import com.hms.appointment.enums.BookingSource;

public record AppointmentResponse(UUID id, UUID patientId, String patientUhid, String patientName, String patientPhone,
		UUID providerId, String providerName, UUID visitTypeId, String visitTypeCode, String visitTypeName,
		int durationMinutes, UUID caseId, LocalDate date, LocalTime startTime, LocalTime endTime,
		AppointmentStatus status, BookingSource source, String note, String cancellationReason, UUID rescheduledFromId,
		LocalDateTime createdAt) {
}
