package com.hms.assessment.service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hms.assessment.dto.TemplateRequest;
import com.hms.assessment.dto.TemplateResponse;
import com.hms.assessment.dto.ValidateTemplateResponse;
import com.hms.assessment.engine.TemplateDefinition;
import com.hms.assessment.engine.TemplateValidator;
import com.hms.assessment.entity.AssessmentTemplate;
import com.hms.assessment.enums.TemplateStatus;
import com.hms.assessment.repository.AssessmentTemplateRepository;
import com.hms.assessment.util.Json;
import com.hms.common.audit.AuditPublisher;
import com.hms.common.exception.BusinessException;
import com.hms.common.exception.DuplicateResourceException;
import com.hms.common.exception.ResourceNotFoundException;
import com.hms.common.security.ClinicScope;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TemplateService {

	private final AssessmentTemplateRepository repository;
	private final AuditPublisher audit;
	private final Clock clock;

	// ------------------------------------------------------- read
	/**
	 * Latest version of every form the clinic can use (its own overrides first,
	 * then shared defaults).
	 */
	@Transactional(readOnly = true)
	public List<TemplateResponse> list() {
		UUID clinic = ClinicScope.currentClinicId();
		Map<String, List<AssessmentTemplate>> byCode = new TreeMap<>();
		for (AssessmentTemplate t : repository.findVisibleTo(clinic)) {
			byCode.computeIfAbsent(t.getCode(), k -> new ArrayList<>()).add(t);
		}
		List<TemplateResponse> out = new ArrayList<>();
		byCode.forEach((code, all) -> {
			List<AssessmentTemplate> own = all.stream().filter(t -> t.getClinicId() != null).toList();
			List<AssessmentTemplate> source = own.isEmpty() ? all : own;
			source.stream().max(Comparator.comparingInt(AssessmentTemplate::getTemplateVersion))
					.ifPresent(t -> out.add(toResponse(t)));
		});
		return out;
	}

	@Transactional(readOnly = true)
	public TemplateResponse get(UUID id) {
		return toResponse(load(id));
	}

	@Transactional(readOnly = true)
	public List<TemplateResponse> versions(String code) {
		UUID clinic = ClinicScope.currentClinicId();
		List<AssessmentTemplate> own = repository.findByClinicIdAndCodeOrderByTemplateVersionDesc(clinic,
				code.toUpperCase());
		List<AssessmentTemplate> list = own.isEmpty() && clinic != null
				? repository.findByClinicIdAndCodeOrderByTemplateVersionDesc(null, code.toUpperCase())
				: own;
		return list.stream().map(this::toResponse).toList();
	}

	/**
	 * The form used for new assessments: the clinic's published version, else the
	 * shared default.
	 */
	@Transactional(readOnly = true)
	public Optional<AssessmentTemplate> resolvePublished(UUID clinicId, String code) {
		Optional<AssessmentTemplate> own = repository.findFirstByClinicIdAndCodeAndStatus(clinicId, code,
				TemplateStatus.PUBLISHED);
		if (own.isPresent() || clinicId == null)
			return own;
		return repository.findFirstByClinicIdAndCodeAndStatus(null, code, TemplateStatus.PUBLISHED);
	}

	// ------------------------------------------------------ write
	@Transactional
	public TemplateResponse create(TemplateRequest r) {
		if (r.code() == null || r.code().isBlank())
			throw new BusinessException("A template code is required");
		UUID clinic = ClinicScope.currentClinicId();
		String code = r.code().trim().toUpperCase();
		if (!repository.findByClinicIdAndCodeOrderByTemplateVersionDesc(clinic, code).isEmpty()) {
			throw new DuplicateResourceException("A template with code " + code + " already exists. Edit it instead.");
		}
		AssessmentTemplate t = new AssessmentTemplate();
		t.setClinicId(clinic);
		t.setCode(code);
		t.setTemplateVersion(1);
		apply(t, r);
		return toResponse(repository.save(t));
	}

	/**
	 * DRAFT: edited in place. PUBLISHED: a new draft version is created (the
	 * published one stays live until you publish).
	 */
	@Transactional
	public TemplateResponse update(UUID id, TemplateRequest r) {
		AssessmentTemplate t = load(id);
		requireEditable(t);
		switch (t.getStatus()) {
		case DRAFT -> {
			apply(t, r);
			return toResponse(t);
		}
		case PUBLISHED -> {
			List<AssessmentTemplate> versions = repository
					.findByClinicIdAndCodeOrderByTemplateVersionDesc(t.getClinicId(), t.getCode());
			Optional<AssessmentTemplate> openDraft = versions.stream()
					.filter(v -> v.getStatus() == TemplateStatus.DRAFT).findFirst();
			if (openDraft.isPresent()) {
				apply(openDraft.get(), r);
				return toResponse(openDraft.get());
			}
			AssessmentTemplate next = new AssessmentTemplate();
			next.setClinicId(t.getClinicId());
			next.setCode(t.getCode());
			next.setVersion(versions.get(0).getVersion() + 1);
			apply(next, r);
			return toResponse(repository.save(next));
		}
		default -> throw new BusinessException(
				"An archived template cannot be edited. Create a new version from the current one.");
		}
	}

	@Transactional
	public TemplateResponse publish(UUID id) {
		AssessmentTemplate t = load(id);
		requireEditable(t);
		if (t.getStatus() != TemplateStatus.DRAFT)
			throw new BusinessException("Only a draft can be published");
		TemplateValidator.parseAndValidate(t.getDefinition());
		repository.findFirstByClinicIdAndCodeAndStatus(t.getClinicId(), t.getCode(), TemplateStatus.PUBLISHED)
				.ifPresent(old -> old.setStatus(TemplateStatus.ARCHIVED));
		t.setStatus(TemplateStatus.PUBLISHED);
		t.setPublishedAt(LocalDateTime.now(clock));
		audit.record("ASSESSMENT_TEMPLATE_PUBLISHED", "AssessmentTemplate", t.getId(),
				Map.of("code", t.getCode(), "version", t.getVersion()));
		return toResponse(t);
	}

	/**
	 * Stops the form being used for NEW assessments. Existing assessments keep
	 * their frozen copy.
	 */
	@Transactional
	public TemplateResponse archive(UUID id) {
		AssessmentTemplate t = load(id);
		requireEditable(t);
		if (t.getStatus() != TemplateStatus.PUBLISHED)
			throw new BusinessException("Only a published template can be archived");
		t.setStatus(TemplateStatus.ARCHIVED);
		audit.record("ASSESSMENT_TEMPLATE_ARCHIVED", "AssessmentTemplate", t.getId(),
				Map.of("code", t.getCode(), "version", t.getVersion()));
		return toResponse(t);
	}

	/**
	 * Copy a shared default into the clinic as an editable draft; once published it
	 * overrides the default.
	 */
	@Transactional
	public TemplateResponse customize(UUID id) {
		UUID clinic = ClinicScope.currentClinicId();
		AssessmentTemplate source = load(id);
		if (source.getClinicId() != null)
			throw new BusinessException("Only shared defaults need to be customised");
		if (clinic == null)
			throw new BusinessException("Edit shared defaults directly; customising is for clinic users");
		if (!repository.findByClinicIdAndCodeOrderByTemplateVersionDesc(clinic, source.getCode()).isEmpty()) {
			throw new DuplicateResourceException("Your clinic already has its own " + source.getCode() + " template");
		}
		AssessmentTemplate copy = new AssessmentTemplate();
		copy.setClinicId(clinic);
		copy.setCode(source.getCode());
		copy.setName(source.getName());
		copy.setDescription(source.getDescription());
		copy.setDefinition(source.getDefinition());
		copy.setTemplateVersion(1);
		return toResponse(repository.save(copy));
	}

	@Transactional(readOnly = true)
	public ValidateTemplateResponse validate(Object definition) {
		try {
			TemplateDefinition d = TemplateValidator.parseAndValidate(Json.write(definition));
			return new ValidateTemplateResponse(true, null, TemplateValidator.countQuestions(d));
		} catch (BusinessException e) {
			return new ValidateTemplateResponse(false, e.getMessage(), 0);
		}
	}

	// -------------------------------------------------- helpers
	private void apply(AssessmentTemplate t, TemplateRequest r) {
		String json = Json.write(r.definition());
		TemplateValidator.parseAndValidate(json);
		t.setName(r.name().trim());
		t.setDescription(r.description());
		t.setDefinition(json);
	}

	private AssessmentTemplate load(UUID id) {
		AssessmentTemplate t = repository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Template", id));
		if (t.getClinicId() != null)
			ClinicScope.assertSameClinic(t.getClinicId(), "Template", id);
		return t;
	}

	/** Shared defaults are read-only for clinic users. */
	private void requireEditable(AssessmentTemplate t) {
		if (t.getClinicId() == null && ClinicScope.currentClinicId() != null) {
			throw new BusinessException("This is a shared default. Customise it for your clinic first.");
		}
	}

	private TemplateResponse toResponse(AssessmentTemplate t) {
		return new TemplateResponse(t.getId(), t.getCode(), t.getName(), t.getDescription(), t.getTemplateVersion(),
				t.getStatus(), t.getClinicId() == null, Json.readMap(t.getDefinition()), t.getPublishedAt(),
				t.getCreatedAt());
	}
}