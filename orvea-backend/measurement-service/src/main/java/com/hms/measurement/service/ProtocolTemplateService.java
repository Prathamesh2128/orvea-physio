package com.hms.measurement.service;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hms.common.exception.BusinessException;
import com.hms.common.exception.ResourceNotFoundException;
import com.hms.common.security.AuthenticatedUser;
import com.hms.common.security.ClinicScope;
import com.hms.common.security.SecurityUtils;
import com.hms.measurement.dto.TemplateDtos.TemplateRequest;
import com.hms.measurement.dto.TemplateDtos.TemplateResponse;
import com.hms.measurement.engine.ProtocolRules;
import com.hms.measurement.entity.ProtocolTemplate;
import com.hms.measurement.enums.MeasurementKind;
import com.hms.measurement.repository.ProtocolTemplateRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ProtocolTemplateService {

	private final ProtocolTemplateRepository repository;

	/** Clinic presets plus the caller's own. */
	@Transactional(readOnly = true)
	public List<TemplateResponse> list() {
		UUID me = userId();
		return repository.findVisible(ClinicScope.currentClinicId(), me).stream().map(t -> toResponse(t, me)).toList();
	}

	@Transactional(readOnly = true)
	public ProtocolTemplate require(UUID id) {
		ProtocolTemplate t = repository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Protocol template", id));
		if (t.getClinicId() != null)
			ClinicScope.assertSameClinic(t.getClinicId(), "Protocol template", id);
		boolean visible = t.isActive() && (t.getOwnerUserId() == null || t.getOwnerUserId().equals(userId()));
		if (!visible)
			throw new ResourceNotFoundException("Protocol template", id);
		return t;
	}

	@Transactional
	public TemplateResponse create(TemplateRequest r) {
		ProtocolRules.validate(r.limbType(), r.stepCm(), r.startLandmark(), r.pointCount());
		boolean shared = Boolean.TRUE.equals(r.shared());
		if (shared && !canShare())
			throw new BusinessException("Only a user who manages measurement settings can create clinic presets");
		ProtocolTemplate t = new ProtocolTemplate();
		t.setClinicId(ClinicScope.currentClinicId());
		t.setOwnerUserId(shared ? null : userId());
		apply(t, r);
		return toResponse(repository.save(t), userId());
	}

	@Transactional
	public TemplateResponse update(UUID id, TemplateRequest r) {
		ProtocolTemplate t = requireEditable(id);
		ProtocolRules.validate(r.limbType(), r.stepCm(), r.startLandmark(), r.pointCount());
		apply(t, r);
		return toResponse(t, userId());
	}

	@Transactional
	public void delete(UUID id) {
		ProtocolTemplate t = requireEditable(id);
		t.setActive(false);
		t.softDelete(SecurityUtils.currentUsername());
	}

	/**
	 * Presets are copied into plans, so changing or removing one never alters
	 * existing cases.
	 */
	private ProtocolTemplate requireEditable(UUID id) {
		ProtocolTemplate t = require(id);
		if (t.getOwnerUserId() == null) {
			if (t.getClinicId() == null && ClinicScope.currentClinicId() != null) {
				throw new BusinessException("This is a starter preset. Create your own copy instead.");
			}
			if (!canShare())
				throw new BusinessException("Only a user who manages measurement settings can change clinic presets");
		}
		return t;
	}

	private void apply(ProtocolTemplate t, TemplateRequest r) {
		t.setName(r.name().trim());
		t.setLimbType(r.limbType());
		t.setStepCm(r.stepCm().setScale(2));
		t.setStartLandmark(r.startLandmark());
		t.setPointCount(r.pointCount());
	}

	private static UUID userId() {
		return SecurityUtils.currentUser().map(AuthenticatedUser::userId).orElse(null);
	}

	private static boolean canShare() {
		return SecurityUtils.currentUser().map(u -> u.permissions().contains("MEASUREMENT_SETTINGS")).orElse(false);
	}

	private TemplateResponse toResponse(ProtocolTemplate t, UUID me) {
		return new TemplateResponse(t.getId(), t.getName(), t.getLimbType(), t.getStepCm(), t.getStartLandmark(),
				t.getPointCount(), ProtocolRules.plannedDistances(t.getStepCm(), t.getPointCount()),
				t.getOwnerUserId() == null, t.getOwnerUserId() != null && t.getOwnerUserId().equals(me),
				ProtocolRules.describe(MeasurementKind.LIMB, t.getLimbType(), t.getStepCm(), t.getStartLandmark(),
						t.getPointCount()));
	}
}