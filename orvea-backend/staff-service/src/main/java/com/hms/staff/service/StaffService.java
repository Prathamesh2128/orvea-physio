package com.hms.staff.service;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hms.common.api.PageResponse;
import com.hms.common.enums.RoleType;
import com.hms.common.exception.BusinessException;
import com.hms.common.exception.DuplicateResourceException;
import com.hms.common.exception.ResourceNotFoundException;
import com.hms.common.security.ClinicScope;
import com.hms.common.security.SecurityUtils;
import com.hms.staff.dto.StaffRef;
import com.hms.staff.dto.StaffRequest;
import com.hms.staff.dto.StaffResponse;
import com.hms.staff.entity.Staff;
import com.hms.staff.enums.StaffStatus;
import com.hms.staff.repository.StaffRepository;
import com.hms.staff.utill.DisplayNameFormatter;
import com.hms.staff.utill.StaffMapper;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class StaffService {

	/** SUPER_ADMIN and PATIENT are accounts, not staff members. */
	private static final Set<RoleType> STAFF_TYPES = EnumSet.of(RoleType.PHYSIOTHERAPIST, RoleType.DOCTOR,
			RoleType.NURSE, RoleType.RECEPTIONIST, RoleType.BILLING_STAFF, RoleType.CLINIC_ASSISTANT, RoleType.ADMIN);

	private final StaffRepository repository;

	@Transactional
	public StaffResponse create(StaffRequest req) {
		requireStaffType(req.staffType());
		if (req.userId() != null && repository.existsByUserId(req.userId())) {
			throw new DuplicateResourceException("This login user is already linked to a staff member");
		}
		Staff s = new Staff();
		s.setClinicId(ClinicScope.currentClinicId());
		s.setUserId(req.userId());
		s.setStatus(StaffStatus.ACTIVE);
		apply(s, req);
		return StaffMapper.toResponse(repository.save(s));
	}

	@Transactional
	public StaffResponse update(UUID id, StaffRequest req) {
		requireStaffType(req.staffType());
		Staff s = find(id);
		if (req.userId() != null && !req.userId().equals(s.getUserId()) && repository.existsByUserId(req.userId())) {
			throw new DuplicateResourceException("This login user is already linked to a staff member");
		}
		s.setUserId(req.userId());
		apply(s, req); // display name is recomputed (type or name may have changed)
		return StaffMapper.toResponse(s);
	}

	@Transactional(readOnly = true)
	public StaffResponse get(UUID id) {
		return StaffMapper.toResponse(find(id));
	}

	@Transactional(readOnly = true)
	public PageResponse<StaffResponse> search(RoleType type, StaffStatus status, String q, Pageable pageable) {
		return PageResponse.from(
				repository.search(type, status, blankToNull(q), ClinicScope.currentClinicId(), pageable),
				StaffMapper::toResponse);
	}

	/** Active physiotherapists and doctors: the "pick a doctor" dropdown. */
	@Transactional(readOnly = true)
	public List<StaffResponse> treatingStaff() {
		return repository.findTreating(StaffMapper.TREATING_TYPES, StaffStatus.ACTIVE, ClinicScope.currentClinicId())
				.stream().map(StaffMapper::toResponse).toList();
	}

	@Transactional
	public StaffResponse setStatus(UUID id, StaffStatus status) {
		Staff s = find(id);
		s.setStatus(status);
		return StaffMapper.toResponse(s);
	}

	@Transactional
	public void delete(UUID id) {
		Staff s = find(id);
		s.setStatus(StaffStatus.INACTIVE);
		s.softDelete(SecurityUtils.currentUsername());
	}

	@Transactional(readOnly = true)
	public StaffRef internalRef(UUID id) {
		return StaffMapper.toRef(find(id));
	}

	// ---------------------------------------------------------- helpers
	private Staff find(UUID id) {
		Staff s = repository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Staff member", id));
		ClinicScope.assertSameClinic(s.getClinicId(), "Staff member", id);
		return s;
	}

	private void requireStaffType(RoleType type) {
		if (!STAFF_TYPES.contains(type)) {
			throw new BusinessException("Invalid staff type: " + type);
		}
	}

	private void apply(Staff s, StaffRequest r) {
		s.setFirstName(r.firstName().trim());
		s.setMiddleName(blankToNull(r.middleName()));
		s.setLastName(r.lastName().trim());
		s.setGender(r.gender());
		s.setStaffType(r.staffType());
		s.setPhone(r.phone());
		s.setEmail(r.email());
		s.setQualification(r.qualification());
		s.setRegistrationNumber(r.registrationNumber());
		s.setSpecialization(r.specialization());
		s.setExperienceYears(r.experienceYears());
		s.setJoiningDate(r.joiningDate());
		s.setDisplayName(DisplayNameFormatter.format(r.staffType(), r.firstName(), r.middleName(), r.lastName()));
	}

	private static String blankToNull(String v) {
		return v == null || v.isBlank() ? null : v.trim();
	}
}