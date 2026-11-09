package com.hms.assessment.service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hms.assessment.client.CaseRef;
import com.hms.assessment.client.PatientClient;
import com.hms.assessment.client.PatientRef;
import com.hms.assessment.dto.AddendumResponse;
import com.hms.assessment.dto.AssessmentResponse;
import com.hms.assessment.dto.AssessmentSummary;
import com.hms.assessment.dto.CreateAssessmentRequest;
import com.hms.assessment.engine.FormEngine;
import com.hms.assessment.engine.Snapshot;
import com.hms.assessment.engine.Snapshot.SnapshotTemplate;
import com.hms.assessment.engine.TemplateDefinition.Question;
import com.hms.assessment.engine.TemplateDefinition.Section;
import com.hms.assessment.engine.TemplateValidator;
import com.hms.assessment.entity.Assessment;
import com.hms.assessment.entity.AssessmentAddendum;
import com.hms.assessment.entity.AssessmentTemplate;
import com.hms.assessment.enums.AssessmentStatus;
import com.hms.assessment.enums.AssessmentType;
import com.hms.assessment.enums.CaptureStatus;
import com.hms.assessment.repository.AssessmentAddendumRepository;
import com.hms.assessment.repository.AssessmentRepository;
import com.hms.assessment.repository.PoseCaptureRepository;
import com.hms.assessment.util.Json;
import com.hms.common.api.PageResponse;
import com.hms.common.audit.AuditPublisher;
import com.hms.common.exception.BusinessException;
import com.hms.common.exception.DuplicateResourceException;
import com.hms.common.exception.ResourceNotFoundException;
import com.hms.common.security.ClinicScope;
import com.hms.common.security.SecurityUtils;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AssessmentService {

	static final String COMMON = "COMMON";
	private static final Set<String> OPEN_CASE_STATUSES = Set.of("PLANNED", "ACTIVE", "ON_HOLD");

	private final AssessmentRepository repository;
	private final AssessmentAddendumRepository addendumRepository;
	private final PoseCaptureRepository captureRepository;
	private final TemplateService templateService;
	private final PatientClient patientClient;
	private final AuditPublisher audit;
	private final Clock clock;

	// ============================================== create
	@Transactional
	public AssessmentResponse create(CreateAssessmentRequest r) {
		PatientRef patient = patientClient.getPatient(r.patientId());
		if (!patient.active())
			throw new BusinessException("Patient record is inactive");
		CaseRef c = patientClient.getCase(r.patientId(), r.caseId());
		if (!OPEN_CASE_STATUSES.contains(c.status())) {
			throw new BusinessException(
					"Assessments can only be started on an open case (this one is " + c.status() + ")");
		}
		LocalDate visitDate = r.visitDate() == null ? LocalDate.now(clock) : r.visitDate();
		AssessmentType type = r.type() != null ? r.type()
				: repository.countByCaseId(c.id()) == 0 ? AssessmentType.INITIAL : AssessmentType.FOLLOW_UP;
		if (repository.existsByCaseIdAndVisitDateAndAssessmentTypeAndStatus(c.id(), visitDate, type,
				AssessmentStatus.DRAFT)) {
			throw new DuplicateResourceException(
					"A draft " + type + " assessment already exists for this case on " + visitDate + ". Continue it.");
		}

		UUID clinic = ClinicScope.currentClinicId();
		Snapshot snapshot = buildSnapshot(clinic, c.conditionCodes());

		Assessment a = new Assessment();
		a.setClinicId(clinic);
		a.setPatientId(patient.id());
		a.setPatientUhid(patient.uhid());
		a.setCaseId(c.id());
		a.setCaseNumber(c.caseNumber());
		a.setAppointmentId(r.appointmentId());
		a.setAssessmentType(type);
		a.setVisitDate(visitDate);
		a.setStatus(AssessmentStatus.DRAFT);
		a.setTemplateSnapshot(Json.write(snapshot));

		if (!Boolean.FALSE.equals(r.copyFromLast())) {
			repository.findFirstByCaseIdAndStatusOrderByVisitDateDescCompletedAtDesc(c.id(), AssessmentStatus.COMPLETED)
					.ifPresent(prev -> {
						List<String> carried = new ArrayList<>();
						Map<String, Map<String, Object>> copied = carry(snapshot, Json.readAnswers(prev.getAnswers()),
								carried);
						a.setAnswers(Json.write(copied));
						a.setCarriedForward(Json.write(carried));
						a.setCopiedFromId(prev.getId());
					});
		}
		Assessment saved = repository.save(a);
		audit.record("ASSESSMENT_CREATED", "Assessment", saved.getId(),
				Map.of("caseId", c.id().toString(), "type", type.name(), "templates", templateCodes(snapshot)));
		return toResponse(saved);
	}

	/**
	 * COMMON first, then one form per condition on the case (conditions without a
	 * published form are skipped).
	 */
	private Snapshot buildSnapshot(UUID clinic, List<String> conditionCodes) {
		List<SnapshotTemplate> list = new ArrayList<>();
		AssessmentTemplate common = templateService.resolvePublished(clinic, COMMON).orElseThrow(
				() -> new BusinessException("No published COMMON assessment template. Publish one first."));
		list.add(snapshotOf(common));
		Set<String> seen = new LinkedHashSet<>();
		seen.add(COMMON);
		if (conditionCodes != null) {
			for (String code : conditionCodes) {
				String key = code == null ? null : code.toUpperCase();
				if (key == null || !seen.add(key))
					continue;
				templateService.resolvePublished(clinic, key).ifPresent(t -> list.add(snapshotOf(t)));
			}
		}
		return new Snapshot(list);
	}

	private SnapshotTemplate snapshotOf(AssessmentTemplate t) {
		return new SnapshotTemplate(t.getCode(), t.getName(),
				t.getTemplateVersion(),
				TemplateValidator.parseAndValidate(t.getDefinition()).sections());
	}

	private List<String> templateCodes(Snapshot s) {
		return s.templates().stream().map(SnapshotTemplate::code).toList();
	}

	/**
	 * Copies stable answers from the last visit (never measurements) that still fit
	 * the current form.
	 */
	private Map<String, Map<String, Object>> carry(Snapshot current, Map<String, Map<String, Object>> previous,
			List<String> carriedKeys) {
		Map<String, Map<String, Object>> out = new LinkedHashMap<>();
		for (SnapshotTemplate t : current.templates()) {
			Map<String, Object> prev = previous.get(t.code());
			if (prev == null)
				continue;
			for (Section s : t.sections()) {
				for (Question q : s.questions()) {
					Object value = prev.get(q.key());
					if (value == null || !FormEngine.carryForwardable(q))
						continue;
					try {
						Object ok = FormEngine.validate(q, value, true);
						if (!FormEngine.isAnswered(q, ok))
							continue;
						out.computeIfAbsent(t.code(), k -> new LinkedHashMap<>()).put(q.key(), ok);
						carriedKeys.add(t.code() + "." + q.key());
					} catch (BusinessException ignored) {
						// the question changed since last visit; leave it blank
					}
				}
			}
		}
		return out;
	}

	// ================================================ draft
	/**
	 * Merges changed answers. Required questions are NOT enforced here (that is
	 * Complete).
	 */
	@Transactional
	public AssessmentResponse saveAnswers(UUID id, Map<String, Map<String, Object>> incoming) {
		Assessment a = requireDraft(id);
		Snapshot snap = snapshot(a);
		Map<String, Map<String, Object>> answers = Json.readAnswers(a.getAnswers());
		Set<String> carried = new LinkedHashSet<>(Json.readStringList(a.getCarriedForward()));

		incoming.forEach((templateCode, questions) -> {
			SnapshotTemplate t = snap.templates().stream().filter(x -> x.code().equals(templateCode)).findFirst()
					.orElseThrow(() -> new BusinessException("Unknown form section: " + templateCode));
			Map<String, Object> target = answers.computeIfAbsent(templateCode, k -> new LinkedHashMap<>());
			questions.forEach((key, raw) -> {
				Question q = question(t, key);
				carried.remove(templateCode + "." + key); // touching it confirms it
				if (raw != null && FormEngine.sameValue(target.get(key), raw))
					return; // unchanged (e.g. a camera value re-sent)
				Object value = FormEngine.validate(q, raw, false);
				if (value == null)
					target.remove(key);
				else
					target.put(key, value);
			});
		});
		a.setAnswers(Json.write(answers));
		a.setCarriedForward(Json.write(new ArrayList<>(carried)));
		return toResponse(repository.save(a));
	}

	/** Used by PoseCaptureService after a clinician confirmed a capture. */
	@Transactional
	public void setCameraAnswer(Assessment a, String questionRef, double degrees, UUID captureId) {
		String[] parts = questionRef.split("\\.", 2);
		Snapshot snap = snapshot(a);
		SnapshotTemplate t = snap.templates().stream().filter(x -> x.code().equals(parts[0])).findFirst().orElseThrow(
				() -> new BusinessException("The linked form section is not part of this assessment: " + questionRef));
		Question q = question(t, parts.length > 1 ? parts[1] : "");
		if (q.type() != com.hms.assessment.engine.QuestionType.ANGLE) {
			throw new BusinessException("The linked question is not an angle question: " + questionRef);
		}
		Map<String, Object> raw = new LinkedHashMap<>();
		raw.put("degrees", degrees);
		raw.put("method", FormEngine.METHOD_CAMERA);
		raw.put("captureId", captureId.toString());
		Object value = FormEngine.validate(q, raw, true);
		Map<String, Map<String, Object>> answers = Json.readAnswers(a.getAnswers());
		answers.computeIfAbsent(t.code(), k -> new LinkedHashMap<>()).put(q.key(), value);
		a.setAnswers(Json.write(answers));
		Set<String> carried = new LinkedHashSet<>(Json.readStringList(a.getCarriedForward()));
		carried.remove(questionRef);
		a.setCarriedForward(Json.write(new ArrayList<>(carried)));
		repository.save(a);
	}

	// ============================================== complete
	@Transactional
	public AssessmentResponse complete(UUID id) {
		Assessment a = requireDraft(id);
		Snapshot snap = snapshot(a);
		Map<String, Map<String, Object>> answers = Json.readAnswers(a.getAnswers());
		List<String> missing = FormEngine.missingRequired(snap, answers);
		if (!missing.isEmpty()) {
			throw new BusinessException("Required questions are not answered: "
					+ String.join("; ", missing.subList(0, Math.min(5, missing.size())))
					+ (missing.size() > 5 ? " (and " + (missing.size() - 5) + " more)" : ""));
		}
		a.setAnswers(Json.write(FormEngine.pruneHidden(snap, answers))); // answers of hidden questions are not part of
																			// the record
		a.setCarriedForward("[]");
		a.setStatus(AssessmentStatus.COMPLETED);
		a.setCompletedAt(LocalDateTime.now(clock));
		a.setCompletedBy(SecurityUtils.currentUsername());
		audit.record("ASSESSMENT_COMPLETED", "Assessment", a.getId(),
				Map.of("caseId", a.getCaseId().toString(), "type", a.getAssessmentType().name()));
		return toResponse(repository.save(a));
	}

	@Transactional
	public AssessmentResponse addAddendum(UUID id, String text) {
		Assessment a = load(id);
		if (a.getStatus() != AssessmentStatus.COMPLETED) {
			throw new BusinessException("Addenda can only be added to a completed assessment; edit the draft instead");
		}
		if (text == null || text.isBlank())
			throw new BusinessException("The addendum text is empty");
		AssessmentAddendum d = new AssessmentAddendum();
		d.setClinicId(a.getClinicId());
		d.setAssessmentId(a.getId());
		d.setText(text.trim());
		d.setAuthor(SecurityUtils.currentUsername());
		addendumRepository.save(d);
		audit.record("ASSESSMENT_ADDENDUM_ADDED", "Assessment", a.getId(), Map.of());
		return toResponse(a);
	}

	/**
	 * Soft delete of a draft only. Completed assessments are medical records and
	 * stay.
	 */
	@Transactional
	public void deleteDraft(UUID id) {
		Assessment a = requireDraft(id);
		String by = SecurityUtils.currentUsername();
		LocalDateTime at = LocalDateTime.now(clock);
		captureRepository.findByAssessmentIdOrderByCreatedAtAsc(id).forEach(c -> c.softDelete(by, at));
		a.softDelete(by, at);
		audit.record("ASSESSMENT_DRAFT_DELETED", "Assessment", id, Map.of());
	}

	// ================================================ read
	@Transactional(readOnly = true)
	public AssessmentResponse get(UUID id) {
		return toResponse(load(id));
	}

	@Transactional(readOnly = true)
	public PageResponse<AssessmentSummary> search(UUID patientId, UUID caseId, AssessmentStatus status,
			Pageable pageable) {
		return PageResponse.from(repository.search(patientId, caseId, status, ClinicScope.currentClinicId(), pageable),
				a -> new AssessmentSummary(a.getId(), a.getCaseId(), a.getCaseNumber(), a.getAssessmentType(),
						a.getVisitDate(), a.getStatus(), a.getCompletedAt(), a.getCompletedBy()));
	}

	// ============================================ helpers
	public Assessment load(UUID id) {
		Assessment a = repository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Assessment", id));
		ClinicScope.assertSameClinic(a.getClinicId(), "Assessment", id);
		return a;
	}

	public Assessment requireDraft(UUID id) {
		Assessment a = load(id);
		if (a.getStatus() != AssessmentStatus.DRAFT) {
			throw new BusinessException("This assessment is completed and locked. Add an addendum instead.");
		}
		return a;
	}

	public Snapshot snapshot(Assessment a) {
		return Json.read(a.getTemplateSnapshot(), Snapshot.class);
	}

	private Question question(SnapshotTemplate t, String key) {
		return t.sections().stream().flatMap(s -> s.questions().stream()).filter(q -> q.key().equals(key)).findFirst()
				.orElseThrow(() -> new BusinessException("Unknown question: " + t.code() + "." + key));
	}

	private AssessmentResponse toResponse(Assessment a) {
		Snapshot snap = snapshot(a);
		Map<String, Map<String, Object>> answers = Json.readAnswers(a.getAnswers());
		List<AddendumResponse> addenda = addendumRepository.findByAssessmentIdOrderByCreatedAtAsc(a.getId()).stream()
				.map(d -> new AddendumResponse(d.getId(), d.getText(), d.getAuthor(), d.getCreatedAt())).toList();
		return new AssessmentResponse(a.getId(), a.getPatientId(), a.getPatientUhid(), a.getCaseId(), a.getCaseNumber(),
				a.getAssessmentType(), a.getVisitDate(), a.getStatus(), a.getVersion(),
				Json.readMap(a.getTemplateSnapshot()), answers, Json.readStringList(a.getCarriedForward()),
				a.getCopiedFromId(), FormEngine.progress(snap, answers),
				captureRepository.countByAssessmentIdAndStatus(a.getId(), CaptureStatus.SUGGESTED), addenda,
				a.getCreatedAt(), a.getCompletedAt(), a.getCompletedBy());
	}
}