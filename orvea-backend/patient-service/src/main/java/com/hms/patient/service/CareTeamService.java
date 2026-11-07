package com.hms.patient.service;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hms.common.exception.BusinessException;
import com.hms.common.exception.ResourceNotFoundException;
import com.hms.common.security.ClinicScope;
import com.hms.common.security.SecurityUtils;
import com.hms.patient.dto.CareTeamRequest;
import com.hms.patient.dto.CareTeamResponse;
import com.hms.patient.entity.CareTeamMember;
import com.hms.patient.entity.Patient;
import com.hms.patient.entity.TreatmentCase;
import com.hms.patient.repository.CareTeamMemberRepository;
import com.hms.patient.repository.PatientRepository;
import com.hms.patient.repository.TreatmentCaseRepository;
import com.hms.patient.util.PatientMapper;
import com.hms.patient.util.PhoneUtil;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CareTeamService {
	private final CareTeamMemberRepository memberRepository;
	private final PatientRepository patientRepository;
	private final TreatmentCaseRepository caseRepository;

	@Transactional
	public CareTeamResponse add(UUID patientId, CareTeamRequest req) {
		Patient patient = loadPatient(patientId);
		CareTeamMember m = new CareTeamMember();
		m.setPatient(patient);
		m.setClinicId(patient.getClinicId());
		apply(m, patient, req);
		return PatientMapper.toResponse(memberRepository.save(m));
	}

	@Transactional(readOnly = true)
	public List<CareTeamResponse> list(UUID patientId) {
		loadPatient(patientId);
		return memberRepository.findByPatientIdOrderByCreatedAtAsc(patientId).stream().map(PatientMapper::toResponse)
				.toList();
	}

	@Transactional
	public CareTeamResponse update(UUID patientId, UUID memberId, CareTeamRequest req) {
		Patient patient = loadPatient(patientId);
		CareTeamMember m = memberRepository.findByIdAndPatientId(memberId, patientId)
				.orElseThrow(() -> new ResourceNotFoundException("Care team member", memberId));
		apply(m, patient, req);
		return PatientMapper.toResponse(m);
	}

	@Transactional
	public void delete(UUID patientId, UUID memberId) {
		loadPatient(patientId);
		memberRepository.findByIdAndPatientId(memberId, patientId)
				.orElseThrow(() -> new ResourceNotFoundException("Care team member", memberId))
				.softDelete(SecurityUtils.currentUsername());
	}

	private void apply(CareTeamMember m, Patient patient, CareTeamRequest r) {
		if (r.caseId() != null) {
			TreatmentCase c = caseRepository.findByIdAndPatientId(r.caseId(), patient.getId())
					.orElseThrow(() -> new BusinessException("Case does not belong to this patient"));
			m.setTreatmentCase(c);
		} else {
			m.setTreatmentCase(null);
		}
		m.setRole(r.role());
		m.setName(r.name().trim());
		m.setPhone(PhoneUtil.normalize(r.phone()));
		m.setEmail(r.email());
		m.setOrganization(r.organization());
		m.setRelationship(r.relationship());
		m.setNotes(r.notes());
		// only one primary contact per patient
		if (r.primaryContact()) {
			memberRepository.findByPatientIdOrderByCreatedAtAsc(patient.getId()).stream().filter(x -> x != m)
					.forEach(x -> x.setPrimaryContact(false));
		}
		m.setPrimaryContact(r.primaryContact());
	}

	private Patient loadPatient(UUID patientId) {
		Patient p = patientRepository.findById(patientId)
				.orElseThrow(() -> new ResourceNotFoundException("Patient", patientId));
		ClinicScope.assertSameClinic(p.getClinicId(), "Patient", patientId);
		return p;
	}
}
