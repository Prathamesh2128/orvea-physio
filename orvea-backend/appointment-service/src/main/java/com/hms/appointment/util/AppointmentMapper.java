package com.hms.appointment.util;

import java.time.Duration;

import com.hms.appointment.dto.AppointmentResponse;
import com.hms.appointment.entity.Appointment;

public final class AppointmentMapper {
	private AppointmentMapper() {
	}

	public static AppointmentResponse toResponse(Appointment a) {
		return new AppointmentResponse(a.getId(), a.getPatientId(), a.getPatientUhid(), a.getPatientName(),
				a.getPatientPhone(), a.getProviderId(), a.getProviderName(), a.getVisitType().getId(),
				a.getVisitType().getCode(), a.getVisitType().getName(),
				(int) Duration.between(a.getStartTime(), a.getEndTime()).toMinutes(), a.getCaseId(),
				a.getAppointmentDate(), a.getStartTime(), a.getEndTime(), a.getStatus(), a.getSource(), a.getNote(),
				a.getCancellationReason(), a.getRescheduledFromId(), a.getCreatedAt());
	}
}
