package com.hms.appointment.service;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hms.appointment.dto.VisitTypeRequest;
import com.hms.appointment.dto.VisitTypeResponse;
import com.hms.appointment.entity.VisitType;
import com.hms.appointment.repository.VisitTypeRepository;
import com.hms.appointment.util.SchedulingRules;
import com.hms.common.exception.DuplicateResourceException;
import com.hms.common.exception.ResourceNotFoundException;
import com.hms.common.security.ClinicScope;
import com.hms.common.security.SecurityUtils;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class VisitTypeService {

	private record Seed(String code, String name, int minutes, boolean counts, int order) {
	}

	private static final List<Seed> DEFAULTS = List.of(new Seed("CONSULTATION", "Consultation", 30, false, 10),
			new Seed("FIRST_ASSESSMENT", "First assessment", 60, false, 20),
			new Seed("THERAPY_SESSION", "Therapy session", 45, true, 30),
			new Seed("LYMPHEDEMA_THERAPY", "Lymphedema therapy", 60, true, 40),
			new Seed("MEASUREMENT_ONLY", "Measurement only", 15, false, 50));

	private final VisitTypeRepository repository;

	/**
	 * Each clinic gets its own editable copy of the defaults the first time it is
	 * used.
	 */
	@Transactional
	public void ensureDefaults(UUID clinicId) {
		if (repository.countByClinicId(clinicId) > 0)
			return;
		for (Seed d : DEFAULTS) {
			VisitType v = new VisitType();
			v.setClinicId(clinicId);
			v.setCode(d.code());
			v.setName(d.name());
			v.setDurationMinutes(d.minutes());
			v.setCountsAsSession(d.counts());
			v.setSortOrder(d.order());
			repository.save(v);
		}
	}

	@Transactional
	public List<VisitTypeResponse> list(boolean activeOnly) {
		UUID clinic = ClinicScope.currentClinicId();
		ensureDefaults(clinic);
		return repository.findByClinicIdOrderBySortOrderAscNameAsc(clinic).stream()
				.filter(v -> !activeOnly || v.isActive()).map(this::toResponse).toList();
	}

	@Transactional(readOnly = true)
	public VisitType require(UUID id) {
		VisitType v = repository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Visit type", id));
		ClinicScope.assertSameClinic(v.getClinicId(), "Visit type", id);
		return v;
	}

	@Transactional
	public VisitTypeResponse create(VisitTypeRequest r) {
		UUID clinic = ClinicScope.currentClinicId();
		String code = r.code().trim().toUpperCase();
		if (repository.findByClinicIdAndCodeIgnoreCase(clinic, code).isPresent()) {
			throw new DuplicateResourceException("Visit type code already exists: " + code);
		}
		VisitType v = new VisitType();
		v.setClinicId(clinic);
		v.setCode(code);
		apply(v, r);
		return toResponse(repository.save(v));
	}

	/** Code is immutable (other records reference it). */
	@Transactional
	public VisitTypeResponse update(UUID id, VisitTypeRequest r) {
		VisitType v = require(id);
		apply(v, r);
		return toResponse(v);
	}

	@Transactional
	public void delete(UUID id) {
		require(id).softDelete(SecurityUtils.currentUsername());
	}

	private void apply(VisitType v, VisitTypeRequest r) {
		v.setName(r.name().trim());
		v.setDurationMinutes(SchedulingRules.requireDuration(r.durationMinutes()));
		v.setCountsAsSession(r.countsAsSession());
		if (r.active() != null)
			v.setActive(r.active());
		if (r.sortOrder() != null)
			v.setSortOrder(r.sortOrder());
	}

	private VisitTypeResponse toResponse(VisitType v) {
		return new VisitTypeResponse(v.getId(), v.getCode(), v.getName(), v.getDurationMinutes(), v.isCountsAsSession(),
				v.isActive(), v.getSortOrder());
	}
}
