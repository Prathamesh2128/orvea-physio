package com.hms.appointment.dto;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

import com.hms.appointment.enums.BookingSource;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Phone-booking form: patient (found by phone search) + doctor + date + slot +
 * visit type + note.
 */
public record BookAppointmentRequest(@NotNull UUID patientId, @NotNull UUID providerId, @NotNull UUID visitTypeId,
		UUID caseId, @NotNull LocalDate date, @NotNull LocalTime startTime, BookingSource source,
		@Size(max = 500) String note) {
}
