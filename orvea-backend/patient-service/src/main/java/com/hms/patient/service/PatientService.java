package com.hms.patient.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hms.common.api.PageResponse;
import com.hms.common.exception.DuplicateResourceException;
import com.hms.common.exception.ResourceNotFoundException;
import com.hms.common.security.AuthenticatedUser;
import com.hms.common.security.ClinicScope;
import com.hms.common.security.SecurityUtils;
import com.hms.patient.dto.DuplicateCandidate;
import com.hms.patient.dto.PatientRequest;
import com.hms.patient.dto.PatientResponse;
import com.hms.patient.dto.PatientSummary;
import com.hms.patient.entity.Patient;
import com.hms.patient.repository.PatientRepository;
import com.hms.patient.repository.TreatmentCaseRepository;
import com.hms.patient.util.PatientMapper;
import com.hms.patient.util.PhoneUtil;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PatientService {
	private final PatientRepository patientRepository;
	private final UhidGenerator uhidGenerator;
	private final TreatmentCaseRepository treatmentCaseRepository;

	@Transactional
	public PatientResponse register(PatientRequest req) {
		UUID clinicId = ClinicScope.currentClinicId();
		if (!Boolean.TRUE.equals(req.confirmDuplicate())) {
			List<DuplicateCandidate> dups = findDuplicates(req, clinicId);
			if (!dups.isEmpty()) {
				String list = dups.stream().map(d -> d.uhid() + " (" + d.fullName() + ")")
						.collect(Collectors.joining(", "));
				throw new DuplicateResourceException("Possible duplicate patient: " + list
						+ ". If this is a different person, resubmit with confirmDuplicate=true.");
			}
		}

		if (req.userId() != null && patientRepository.existsByUserId(req.userId())) {
			throw new DuplicateResourceException("This login user is already linked to a patient");
		}

		Patient p = new Patient();
		apply(p, req);
		p.setClinicId(clinicId);
		p.setUserId(req.userId());
		p.setUhid(uhidGenerator.generate(clinicId));
		p.setRegisteredOn(LocalDate.now());
		return PatientMapper.toResponse(patientRepository.save(p));
	}

	@Transactional
	public List<DuplicateCandidate> findDuplicates(PatientRequest req, UUID clinicId) {
		String first = req.firstName().trim();
		String last = req.lastName() == null ? "" : req.lastName().trim();
		String phone = PhoneUtil.normalize(req.phone());

		Map<UUID, Patient> patients = new LinkedHashMap<>();
		Map<UUID, List<String>> reasons = new LinkedHashMap<>();

		patientRepository.findByNameAndDob(first, last, req.dateOfBirth(), clinicId)
				.forEach(p -> note(patients, reasons, p, "Same name and date of birth"));
		patientRepository.findByPhoneAndFirstName(phone, first, clinicId)
				.forEach(p -> note(patients, reasons, p, "Same phone number and first name"));
		if (req.email() != null && !req.email().isBlank()) {
			patientRepository.findByEmail(req.email(), clinicId).forEach(p -> note(patients, reasons, p, "Same email"));
		}
		return patients.values().stream().map(p -> new DuplicateCandidate(p.getId(), p.getUhid(), p.getFullName(),
				p.getDateOfBirth(), p.getPhone(), reasons.get(p.getId()))).toList();
	}

	@Transactional
	public PageResponse<PatientSummary> search(String phone, String name, String uhid, Pageable pageable) {
		Page<Patient> page = patientRepository.search(PhoneUtil.normalize(phone), blankToNull(name),
				uhid == null || uhid.isBlank() ? null : uhid.trim().toUpperCase(), ClinicScope.currentClinicId(),
				pageable);
		return PageResponse.from(page, PatientMapper::toSummary);
	}

	/** For the PATIENT role: the record linked to the logged-in user. */
	@Transactional(readOnly = true)
	public PatientResponse getMine() {
		UUID userId = SecurityUtils.currentUser().map(AuthenticatedUser::userId)
				.orElseThrow(() -> new ResourceNotFoundException("Patient", "Current user"));
		return patientRepository.findByUserId(userId).map(PatientMapper::toResponse)
				.orElseThrow(() -> new ResourceNotFoundException("Patient for user", userId));
	}

	@Transactional(readOnly = true)
	public PatientResponse getByUhid(String uhid) {
		return patientRepository.findByUhidIgnoreCaseAndClinicId(uhid.trim(), ClinicScope.currentClinicId())
				.map(PatientMapper::toResponse).orElseThrow(() -> new ResourceNotFoundException("Patient", uhid));
	}

	@Transactional(readOnly = true)
	public PatientResponse get(UUID id) {
		return PatientMapper.toResponse(find(id));
	}

	@Transactional
	public PatientResponse update(UUID id, PatientRequest req) {
		Patient p = find(id);
		if (req.userId() != null && !req.userId().equals(p.getUserId())
				&& patientRepository.existsByUserId(req.userId())) {
			throw new DuplicateResourceException("This login user is already linked to a patient");
		}
		apply(p, req);
		p.setUserId(req.userId());
		return PatientMapper.toResponse(p);
	}

	@Transactional
	public void delete(UUID id) {
		Patient p = find(id);
		String by = SecurityUtils.currentUsername();
		treatmentCaseRepository.findByPatientId(id).forEach(c -> c.softDelete(by));
		p.softDelete(by);
	}

	// Helper Methods
	public void note(Map<UUID, Patient> patients, Map<UUID, List<String>> reasons, Patient p, String reason) {
		patients.putIfAbsent(p.getId(), p);
		reasons.computeIfAbsent(p.getId(), k -> new ArrayList<>()).add(reason);
	}

	private void apply(Patient p, PatientRequest r) {
		p.setFirstName(r.firstName().trim());
		p.setLastName(blankToNull(r.lastName()));
		p.setGender(r.gender());
		p.setDateOfBirth(r.dateOfBirth());
		p.setDobApproximate(r.dobApproximate());
		p.setBloodGroup(blankToNull(r.bloodGroup()));
		p.setCountryCode(r.countryCode() == null || r.countryCode().isBlank() ? "+91" : r.countryCode().trim());
		p.setPhone(PhoneUtil.normalize(r.phone()));
		p.setAlternatePhone(PhoneUtil.normalize(r.alternatePhone()));
		p.setEmail(r.email() == null || r.email().isBlank() ? null : r.email().trim().toLowerCase());
		p.setAddressLine(blankToNull(r.addressLine()));
		p.setCity(blankToNull(r.city()));
		p.setState(blankToNull(r.state()));
		p.setPincode(blankToNull(r.pincode()));
		p.setOccupation(blankToNull(r.occupation()));
		p.setReferredBy(blankToNull(r.referredBy()));
		p.setEmergencyContactName(blankToNull(r.emergencyContactName()));
		p.setEmergencyContactPhone(PhoneUtil.normalize(r.emergencyContactPhone()));
		p.setEmergencyContactRelation(blankToNull(r.emergencyContactRelation()));
		p.setNotes(blankToNull(r.notes()));
	}

	private static String blankToNull(String s) {
		return s == null || s.isBlank() ? null : s.trim();
	}

	Patient find(UUID id) {
		Patient p = patientRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Patient", id));
		ClinicScope.assertSameClinic(p.getClinicId(), "Patient", id);
		return p;
	}
}
