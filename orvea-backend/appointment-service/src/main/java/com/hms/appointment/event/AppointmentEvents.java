package com.hms.appointment.event;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.hms.appointment.entity.Appointment;
import com.hms.appointment.enums.AppointmentStatus;
import com.hms.common.audit.AuditPublisher;
import com.hms.common.event.EventPublisher;
import com.hms.common.event.EventTopics;
import com.hms.common.event.EventTypes;

import lombok.RequiredArgsConstructor;

/**
 * Business events (for WhatsApp messages) and audit records for appointments.
 */
@Component
@RequiredArgsConstructor
public class AppointmentEvents {

	private final EventPublisher events;
	private final AuditPublisher audit;

	public void booked(Appointment a) {
		events.publish(EventTopics.APPOINTMENT, a.getId().toString(), EventTypes.APPOINTMENT_BOOKED, a.getClinicId(),
				payload(a));
		audit.record("APPOINTMENT_BOOKED", "Appointment", a.getId(), auditDetails(a));
	}

	public void rescheduled(Appointment old, Appointment next) {
		Map<String, Object> p = payload(next);
		p.put("rescheduledFromId", old.getId().toString());
		events.publish(EventTopics.APPOINTMENT, next.getId().toString(), EventTypes.APPOINTMENT_RESCHEDULED,
				next.getClinicId(), p);
		Map<String, Object> d = auditDetails(next);
		d.put("rescheduledFromId", old.getId().toString());
		d.put("previousDate", old.getAppointmentDate().toString());
		d.put("previousStart", old.getStartTime().toString());
		audit.record("APPOINTMENT_RESCHEDULED", "Appointment", next.getId(), d);
	}

	public void cancelled(Appointment a) {
		events.publish(EventTopics.APPOINTMENT, a.getId().toString(), EventTypes.APPOINTMENT_CANCELLED, a.getClinicId(),
				payload(a));
		Map<String, Object> d = auditDetails(a);
		d.put("reason", a.getCancellationReason());
		audit.record("APPOINTMENT_CANCELLED", "Appointment", a.getId(), d);
	}

	/**
	 * Audit only: confirmations, check-in, completion and no-shows need no patient
	 * message.
	 */
	public void statusChanged(Appointment a, AppointmentStatus from, AppointmentStatus to) {
		Map<String, Object> d = auditDetails(a);
		d.put("from", from.name());
		d.put("to", to.name());
		audit.record("APPOINTMENT_STATUS_CHANGED", "Appointment", a.getId(), d);
	}

	Map<String, Object> payload(Appointment a) {
		Map<String, Object> p = new LinkedHashMap<>();
		p.put("appointmentId", a.getId().toString());
		p.put("patientId", a.getPatientId().toString());
		p.put("patientName", a.getPatientName());
		p.put("patientPhone", a.getPatientPhone());
		p.put("providerName", a.getProviderName());
		p.put("visitTypeName", a.getVisitType().getName());
		p.put("appointmentAt", a.getAppointmentDate().atTime(a.getStartTime()).toString());
		return p;
	}

	private Map<String, Object> auditDetails(Appointment a) {
		Map<String, Object> d = new LinkedHashMap<>();
		d.put("patientId", a.getPatientId().toString());
		d.put("providerId", a.getProviderId().toString());
		d.put("date", a.getAppointmentDate().toString());
		d.put("start", a.getStartTime().toString());
		d.put("status", a.getStatus().name());
		return d;
	}

	/**
	 * Billing listens to this: a completed visit is either covered by a package or
	 * becomes a pending charge.
	 */
	public void completed(Appointment a) {
		Map<String, Object> p = new LinkedHashMap<>();
		p.put("appointmentId", a.getId().toString());
		p.put("patientId", a.getPatientId().toString());
		p.put("patientName", a.getPatientName());
		p.put("patientUhid", a.getPatientUhid());
		p.put("caseId", a.getCaseId() == null ? null : a.getCaseId().toString());
		p.put("providerId", a.getProviderId().toString());
		p.put("providerName", a.getProviderName());
		p.put("visitTypeCode", a.getVisitType().getCode());
		p.put("visitTypeName", a.getVisitType().getName());
		p.put("durationMinutes", (int) Duration.between(a.getStartTime(), a.getEndTime()).toMinutes());
		p.put("countsAsSession", a.getVisitType().isCountsAsSession());
		p.put("appointmentDate", a.getAppointmentDate().toString());
		events.publish(EventTopics.APPOINTMENT, a.getId().toString(), EventTypes.APPOINTMENT_COMPLETED, a.getClinicId(), p);
	}

}
