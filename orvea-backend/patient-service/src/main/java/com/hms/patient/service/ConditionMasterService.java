package com.hms.patient.service;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hms.common.exception.DuplicateResourceException;
import com.hms.common.exception.ResourceNotFoundException;
import com.hms.common.security.SecurityUtils;
import com.hms.patient.dto.ConditionMasterRequest;
import com.hms.patient.dto.ConditionMasterResponse;
import com.hms.patient.entity.ConditionMaster;
import com.hms.patient.repository.ConditionMasterRepository;
import com.hms.patient.util.PatientMapper;

import lombok.RequiredArgsConstructor;

/** New conditions are added as data. No code change or redeploy needed. */
@Service
@RequiredArgsConstructor
public class ConditionMasterService {

	private final ConditionMasterRepository repository;

	@Transactional(readOnly = true)
	public List<ConditionMasterResponse> list(boolean activeOnly) {
		return (activeOnly ? repository.findByActiveTrueOrderBySortOrderAscNameAsc()
				: repository.findAllByOrderBySortOrderAscNameAsc()).stream().map(PatientMapper::toResponse).toList();
	}

	@Transactional
	public ConditionMasterResponse create(ConditionMasterRequest req) {
		String code = req.code().trim().toUpperCase();
		if (repository.existsByCodeIgnoreCase(code)) {
			throw new DuplicateResourceException("Condition code already exists: " + code);
		}
		ConditionMaster m = new ConditionMaster();
		m.setCode(code);
		apply(m, req);
		return PatientMapper.toResponse(repository.save(m));
	}

	/**
	 * The code is immutable because cases and assessment templates reference it.
	 */
	@Transactional
	public ConditionMasterResponse update(UUID id, ConditionMasterRequest req) {
		ConditionMaster m = find(id);
		apply(m, req);
		return PatientMapper.toResponse(m);
	}

	@Transactional
	public void delete(UUID id) {
		find(id).softDelete(SecurityUtils.currentUsername());
	}

	private ConditionMaster find(UUID id) {
		return repository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Condition", id));
	}

	private void apply(ConditionMaster m, ConditionMasterRequest r) {
		m.setName(r.name().trim());
		m.setCategory(r.category());
		m.setDescription(r.description());
		if (r.active() != null)
			m.setActive(r.active());
		if (r.sortOrder() != null)
			m.setSortOrder(r.sortOrder());
	}
}
