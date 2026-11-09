package com.hms.measurement.service;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hms.common.event.EventPublisher;
import com.hms.common.event.EventTopics;
import com.hms.common.event.EventTypes;
import com.hms.measurement.client.PatientClient;
import com.hms.measurement.client.PatientRef;
import com.hms.measurement.entity.MeasurementPlan;
import com.hms.measurement.entity.MeasurementSettings;
import com.hms.measurement.enums.PlanEventType;
import com.hms.measurement.enums.PlanStatus;
import com.hms.measurement.repository.PlanRepository;

import lombok.RequiredArgsConstructor;

/**
 * Optional patient reminders. Sending, consent and the clinic's on/off switch
 * are handled by notification-service.
 */
@Service
@RequiredArgsConstructor
public class MeasurementReminderService {

	private final PlanRepository plans;
	private final SettingsService settingsService;
	private final PatientClient patientClient;
	private final EventPublisher eventPublisher;
	private final PlanEvents events;

	@Transactional(readOnly = true)
	public List<UUID> candidateIds(LocalDate today) {
		return plans.findReminderCandidateIds(PlanStatus.ACTIVE, today, today.plusDays(14));
	}

	/** @return true when a reminder was handed to notification-service. */
	@Transactional
	public boolean remindOne(UUID planId, LocalDate today) {
		MeasurementPlan p = plans.findById(planId).orElse(null);
		if (p == null || p.getStatus() != PlanStatus.ACTIVE || !p.isRemindPatient() || p.getNextDueDate() == null)
			return false;
		MeasurementSettings s = settingsService.effective(p.getClinicId());
		if (!p.getNextDueDate().equals(today.plusDays(s.getPatientReminderLeadDays())))
			return false; // not the lead day
		if (p.getNextDueDate().equals(p.getLastReminderDueDate()))
			return false; // already queued
		PatientRef patient = patientClient.getPatientAsSystem(p.getPatientId(), p.getClinicId());
		if (!patient.active())
			return false;

		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("planId", p.getId().toString());
		payload.put("patientId", p.getPatientId().toString());
		payload.put("patientName", patient.fullName());
		payload.put("patientPhone", patient.phone());
		payload.put("dueDate", p.getNextDueDate().toString());
		eventPublisher.publishNow(EventTopics.NOTIFICATION, p.getId().toString(), EventTypes.MEASUREMENT_DUE,
				p.getClinicId(), payload);

		p.setLastReminderDueDate(p.getNextDueDate());
		plans.save(p);
		events.log(p, PlanEventType.REMINDER_QUEUED, today, null, Map.of("dueDate", p.getNextDueDate().toString()));
		return true;
	}
}