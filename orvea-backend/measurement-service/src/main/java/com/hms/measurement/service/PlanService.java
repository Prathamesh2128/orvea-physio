package com.hms.measurement.service;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hms.common.api.PageResponse;
import com.hms.common.audit.AuditPublisher;
import com.hms.common.exception.BusinessException;
import com.hms.common.exception.DuplicateResourceException;
import com.hms.common.exception.ResourceNotFoundException;
import com.hms.common.security.ClinicScope;
import com.hms.measurement.client.CaseRef;
import com.hms.measurement.client.PatientClient;
import com.hms.measurement.client.PatientRef;
import com.hms.measurement.dto.PlanDtos.ChangeProtocolRequest;
import com.hms.measurement.dto.PlanDtos.CreatePlanRequest;
import com.hms.measurement.dto.PlanDtos.EventView;
import com.hms.measurement.dto.PlanDtos.FormItem;
import com.hms.measurement.dto.PlanDtos.PlanForm;
import com.hms.measurement.dto.PlanDtos.PlanResponse;
import com.hms.measurement.dto.PlanDtos.ProtocolView;
import com.hms.measurement.dto.PlanDtos.ScheduleView;
import com.hms.measurement.dto.PlanDtos.SnoozeRequest;
import com.hms.measurement.dto.PlanDtos.UpdatePlanRequest;
import com.hms.measurement.engine.DueCalculator;
import com.hms.measurement.engine.FaceProtocol;
import com.hms.measurement.engine.ProtocolRules;
import com.hms.measurement.engine.Results;
import com.hms.measurement.entity.MeasurementPlan;
import com.hms.measurement.entity.MeasurementSettings;
import com.hms.measurement.entity.ProtocolTemplate;
import com.hms.measurement.enums.Landmark;
import com.hms.measurement.enums.LimbType;
import com.hms.measurement.enums.MeasurementKind;
import com.hms.measurement.enums.PlanEventType;
import com.hms.measurement.enums.PlanStatus;
import com.hms.measurement.enums.RecordStatus;
import com.hms.measurement.enums.Side;
import com.hms.measurement.repository.PlanRepository;
import com.hms.measurement.repository.RecordRepository;
import com.hms.measurement.util.Json;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PlanService {

	private static final Set<String> OPEN_CASE = Set.of("PLANNED", "ACTIVE", "ON_HOLD");

	private final PlanRepository plans;
	private final RecordRepository records;
	private final PlanEvents events;
	private final ProtocolTemplateService templates;
	private final SettingsService settingsService;
	private final TrendService trendService;
	private final PatientClient patientClient;
	private final AuditPublisher audit;
	private final Clock clock;

	// ============================================== create
	@Transactional
	public PlanResponse create(CreatePlanRequest r) {
		PatientRef patient = patientClient.getPatient(r.patientId());
		if (!patient.active())
			throw new BusinessException("Patient record is inactive");
		CaseRef c = patientClient.getCase(r.patientId(), r.caseId());
		if (!OPEN_CASE.contains(c.status())) {
			throw new BusinessException(
					"Measurements can only be planned on an open case (this one is " + c.status() + ")");
		}
		UUID clinic = ClinicScope.currentClinicId();
		LocalDate today = today();
		MeasurementSettings s = settingsService.effective(clinic);

		MeasurementPlan p = new MeasurementPlan();
		p.setClinicId(clinic);
		p.setPatientId(patient.id());
		p.setPatientUhid(patient.uhid());
		p.setPatientName(patient.fullName());
		p.setCaseId(c.id());
		p.setCaseNumber(c.caseNumber());
		p.setKind(r.kind());
		p.setStartedOn(today);
		p.setStatus(PlanStatus.ACTIVE);

		if (r.kind() == MeasurementKind.LIMB) {
			ProtocolTemplate t = r.templateId() == null ? null : templates.require(r.templateId());
			LimbType limb = t != null ? t.getLimbType() : r.limbType();
			BigDecimal step = t != null ? t.getStepCm() : r.stepCm();
			Landmark landmark = t != null ? t.getStartLandmark() : r.startLandmark();
			Integer points = t != null ? Integer.valueOf(t.getPointCount()) : r.pointCount();
			ProtocolRules.validate(limb, step, landmark, points);
			if (plans.existsByCaseIdAndKindAndLimbTypeAndStatusNot(c.id(), MeasurementKind.LIMB, limb,
					PlanStatus.ENDED)) {
				throw new DuplicateResourceException(
						"This case already has a " + limb.name().toLowerCase() + " measurement plan");
			}
			p.setLimbType(limb);
			p.setStepCm(step.setScale(2));
			p.setStartLandmark(landmark);
			p.setPointCount(points);
		} else if (plans.existsByCaseIdAndKindAndLimbTypeAndStatusNot(c.id(), MeasurementKind.FACE, null,
				PlanStatus.ENDED)) {
			throw new DuplicateResourceException("This case already has a face measurement plan");
		}

		p.setAffectedSide(r.affectedSide() != null ? r.affectedSide() : sideFromCase(c.laterality()));
		p.setRepeatDays(
				r.repeatDays() != null ? SettingsService.checkRepeat(r.repeatDays()) : s.getDefaultRepeatDays());
		p.setDueMode(r.dueMode() != null ? r.dueMode() : s.getDefaultDueMode());
		p.setRemindPatient(Boolean.TRUE.equals(r.remindPatient()));
		if (r.firstDueDate() != null && r.firstDueDate().isBefore(today))
			throw new BusinessException("The first due date cannot be in the past");
		p.setNextDueDate(r.firstDueDate() != null ? r.firstDueDate() : today);
		p.setTherapistId(r.therapistId() != null ? r.therapistId() : c.primaryTherapistId());
		p.setNote(r.note());

		MeasurementPlan saved = plans.save(p);
		events.log(saved, PlanEventType.CREATED, today, null, Map.of("kind", r.kind().name()));
		audit.record("MEASUREMENT_PLAN_CREATED", "MeasurementPlan", saved.getId(),
				Map.of("caseId", c.id().toString(), "kind", r.kind().name()));
		return toResponse(saved, s);
	}

	private static Side sideFromCase(String laterality) {
		return "LEFT".equals(laterality) ? Side.LEFT : "RIGHT".equals(laterality) ? Side.RIGHT : null;
	}

	// ============================================== read
	@Transactional
	public PlanResponse get(UUID id) {
		MeasurementPlan p = load(id);
		return toResponse(p, settingsService.effective(p.getClinicId()));
	}

	@Transactional(readOnly = true)
	public PageResponse<PlanResponse> search(UUID patientId, UUID caseId, PlanStatus status, Pageable pageable) {
		MeasurementSettings s = settingsService.effective(ClinicScope.currentClinicId());
		return PageResponse.from(plans.search(patientId, caseId, status, ClinicScope.currentClinicId(), pageable),
				p -> toResponse(p, s));
	}

	@Transactional(readOnly = true)
	public List<EventView> history(UUID id) {
		load(id);
		return events.list(id);
	}

	/** "Measure now": the saved protocol plus the previous values. */
	@Transactional(readOnly = true)
	public PlanForm form(UUID id) {
		MeasurementPlan p = load(id);
		if (p.getStatus() == PlanStatus.ENDED)
			throw new BusinessException("This plan has ended");
		List<FormItem> items = new ArrayList<>();
		List<FormItem> angles = new ArrayList<>();
		if (p.getKind() == MeasurementKind.LIMB) {
			List<BigDecimal> planned = ProtocolRules.plannedDistances(p.getStepCm(), p.getPointCount());
			for (int i = 0; i < planned.size(); i++) {
				items.add(new FormItem(String.valueOf(i + 1),
						ProtocolRules.pointLabel(p.getStartLandmark(), planned.get(i)), i + 1, planned.get(i)));
			}
		} else {
			FaceProtocol.LANDMARKS.forEach(l -> items.add(new FormItem(l.key(), l.label(), null, null)));
			FaceProtocol.ANGLES.forEach(a -> angles.add(new FormItem(a.key(), a.label(), null, null)));
		}
		Results last = records.findRecent(p.getId(), RecordStatus.ACTIVE, PageRequest.of(0, 5)).stream()
				.filter(r -> r.getProtocolRevision() == p.getProtocolRevision()).findFirst()
				.map(r -> Json.read(r.getResults(), Results.class)).orElse(null);
		return new PlanForm(
				p.getId(), p.getKind(), p.getAffectedSide(), p.getProtocolRevision(), ProtocolRules
						.describe(p.getKind(), p.getLimbType(), p.getStepCm(), p.getStartLandmark(), p.getPointCount()),
				items, angles, last);
	}

	// ============================================== change
	@Transactional
	public PlanResponse update(UUID id, UpdatePlanRequest r) {
		MeasurementPlan p = load(id);
		requireNotEnded(p);
		LocalDate today = today();
		Map<String, Object> changed = new LinkedHashMap<>();
		if (r.repeatDays() != null) {
			p.setRepeatDays(SettingsService.checkRepeat(r.repeatDays()));
			changed.put("repeatDays", r.repeatDays());
		}
		if (r.dueMode() != null) {
			p.setDueMode(r.dueMode());
			changed.put("dueMode", r.dueMode().name());
		}
		if (r.remindPatient() != null) {
			p.setRemindPatient(r.remindPatient());
			changed.put("remindPatient", r.remindPatient());
		}
		if (r.nextDueDate() != null) {
			if (r.nextDueDate().isBefore(today))
				throw new BusinessException("The next due date cannot be in the past");
			p.setNextDueDate(r.nextDueDate());
			changed.put("nextDueDate", r.nextDueDate().toString());
		}
		if (r.therapistId() != null) {
			p.setTherapistId(r.therapistId());
			changed.put("therapistId", r.therapistId().toString());
		}
		if (r.affectedSide() != null) {
			p.setAffectedSide(r.affectedSide());
			changed.put("affectedSide", r.affectedSide().name());
		}
		if (r.note() != null)
			p.setNote(r.note());
		if (!changed.isEmpty())
			events.log(p, PlanEventType.SCHEDULE_CHANGED, today, null, changed);
		return toResponse(plans.save(p), settingsService.effective(p.getClinicId()));
	}

	/**
	 * Once readings exist the change must be confirmed (409 first), needs a reason,
	 * and starts a new revision; old readings keep their own protocol and are not
	 * mixed into comparisons.
	 */
	@Transactional
	public PlanResponse changeProtocol(UUID id, ChangeProtocolRequest r) {
		MeasurementPlan p = load(id);
		requireNotEnded(p);
		if (p.getKind() != MeasurementKind.LIMB)
			throw new BusinessException("The face protocol is fixed");
		BigDecimal step = r.stepCm() != null ? r.stepCm() : p.getStepCm();
		Landmark landmark = r.startLandmark() != null ? r.startLandmark() : p.getStartLandmark();
		Integer points = r.pointCount() != null ? r.pointCount() : p.getPointCount();
		ProtocolRules.validate(p.getLimbType(), step, landmark, points);
		if (step.compareTo(p.getStepCm()) == 0 && landmark == p.getStartLandmark()
				&& points.equals(p.getPointCount())) {
			throw new BusinessException("The protocol is unchanged");
		}
		long existing = records.countByPlanIdAndStatus(p.getId(), RecordStatus.ACTIVE);
		Map<String, Object> details = new LinkedHashMap<>();
		details.put("previous", ProtocolRules.describe(p.getKind(), p.getLimbType(), p.getStepCm(),
				p.getStartLandmark(), p.getPointCount()));
		details.put("previousRevision", p.getProtocolRevision());
		if (existing > 0) {
			if (!Boolean.TRUE.equals(r.confirmChange())) {
				throw new BusinessException("This plan already has " + existing
						+ " measurement(s). Changing the protocol means new "
						+ "readings can no longer be compared point by point with the old ones. Send confirmChange=true "
						+ "and a reason to continue.", HttpStatus.CONFLICT);
			}
			if (r.reason() == null || r.reason().isBlank())
				throw new BusinessException("A reason is required to change the protocol mid-treatment");
			p.setProtocolRevision(p.getProtocolRevision() + 1);
		}
		p.setStepCm(step.setScale(2));
		p.setStartLandmark(landmark);
		p.setPointCount(points);
		details.put("new", ProtocolRules.describe(p.getKind(), p.getLimbType(), p.getStepCm(), p.getStartLandmark(),
				p.getPointCount()));
		details.put("newRevision", p.getProtocolRevision());
		events.log(p, PlanEventType.PROTOCOL_CHANGED, today(), r.reason(), details);
		audit.record("MEASUREMENT_PROTOCOL_CHANGED", "MeasurementPlan", p.getId(),
				Map.of("revision", p.getProtocolRevision()));
		return toResponse(plans.save(p), settingsService.effective(p.getClinicId()));
	}

	@Transactional
	public PlanResponse snooze(UUID id, SnoozeRequest r) {
		MeasurementPlan p = load(id);
		requireActive(p);
		LocalDate until = DueCalculator.checkSnooze(r.until(), today());
		LocalDate from = p.getNextDueDate();
		p.setNextDueDate(until);
		events.log(p, PlanEventType.SNOOZED, today(), r.reason().trim(),
				Map.of("from", String.valueOf(from), "until", until.toString()));
		return toResponse(plans.save(p), settingsService.effective(p.getClinicId()));
	}

	/**
	 * "Not needed" for this occurrence. The next one follows the plan's due mode.
	 */
	@Transactional
	public PlanResponse skip(UUID id, String reason) {
		MeasurementPlan p = load(id);
		requireActive(p);
		LocalDate skipped = p.getNextDueDate();
		p.setNextDueDate(DueCalculator.afterSkip(p.getDueMode(), skipped, today(), p.getRepeatDays()));
		events.log(p, PlanEventType.SKIPPED, today(), reason.trim(),
				Map.of("skippedDue", String.valueOf(skipped), "nextDue", p.getNextDueDate().toString()));
		return toResponse(plans.save(p), settingsService.effective(p.getClinicId()));
	}

	@Transactional
	public PlanResponse pause(UUID id, String reason) {
		MeasurementPlan p = load(id);
		requireActive(p);
		p.setStatus(PlanStatus.PAUSED);
		events.log(p, PlanEventType.PAUSED, today(), reason.trim(), null);
		return toResponse(plans.save(p), settingsService.effective(p.getClinicId()));
	}

	@Transactional
	public PlanResponse resume(UUID id) {
		MeasurementPlan p = load(id);
		if (p.getStatus() != PlanStatus.PAUSED)
			throw new BusinessException("Only a paused plan can be resumed");
		p.setStatus(PlanStatus.ACTIVE);
		if (p.getNextDueDate() == null || p.getNextDueDate().isBefore(today()))
			p.setNextDueDate(today());
		events.log(p, PlanEventType.RESUMED, today(), null, null);
		return toResponse(plans.save(p), settingsService.effective(p.getClinicId()));
	}

	@Transactional
	public PlanResponse end(UUID id, String reason) {
		MeasurementPlan p = load(id);
		requireNotEnded(p);
		p.setStatus(PlanStatus.ENDED);
		events.log(p, PlanEventType.ENDED, today(), reason.trim(), null);
		audit.record("MEASUREMENT_PLAN_ENDED", "MeasurementPlan", p.getId(), Map.of());
		return toResponse(plans.save(p), settingsService.effective(p.getClinicId()));
	}

	// ============================================== helpers
	public MeasurementPlan load(UUID id) {
		MeasurementPlan p = plans.findById(id).orElseThrow(() -> new ResourceNotFoundException("Measurement plan", id));
		ClinicScope.assertSameClinic(p.getClinicId(), "Measurement plan", id);
		return p;
	}

	private void requireNotEnded(MeasurementPlan p) {
		if (p.getStatus() == PlanStatus.ENDED)
			throw new BusinessException("This plan has ended");
	}

	private void requireActive(MeasurementPlan p) {
		if (p.getStatus() != PlanStatus.ACTIVE)
			throw new BusinessException("The plan is " + p.getStatus() + ", not active");
	}

	private LocalDate today() {
		return LocalDate.now(clock);
	}

	PlanResponse toResponse(MeasurementPlan p, MeasurementSettings s) {
		List<BigDecimal> planned = p.getKind() == MeasurementKind.LIMB
				? ProtocolRules.plannedDistances(p.getStepCm(), p.getPointCount())
				: List.of();
		ProtocolView protocol = new ProtocolView(p.getKind(), p.getLimbType(), p.getStepCm(), p.getStartLandmark(),
				p.getPointCount(), p.getProtocolRevision(), planned, ProtocolRules.describe(p.getKind(),
						p.getLimbType(), p.getStepCm(), p.getStartLandmark(), p.getPointCount()));
		ScheduleView schedule = new ScheduleView(p.getRepeatDays(), p.getDueMode(), p.getNextDueDate(),
				p.getLastMeasuredOn(), p.isRemindPatient());
		return new PlanResponse(p.getId(), p.getPatientId(), p.getPatientUhid(), p.getPatientName(), p.getCaseId(),
				p.getCaseNumber(), p.getTherapistId(), p.getStatus(), p.getAffectedSide(), protocol, schedule,
				trendService.lastResult(p, s), p.getNote(), p.getStartedOn());
	}
}