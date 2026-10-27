package com.hms.patient.service;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hms.common.exception.BusinessException;
import com.hms.common.exception.DuplicateResourceException;
import com.hms.common.exception.ResourceNotFoundException;
import com.hms.common.security.ClinicScope;
import com.hms.common.security.SecurityUtils;
import com.hms.patient.dto.CaseRequest;
import com.hms.patient.dto.CaseResponse;
import com.hms.patient.dto.CaseStatusRequest;
import com.hms.patient.dto.ConditionRequest;
import com.hms.patient.entity.CaseCondition;
import com.hms.patient.entity.ConditionMaster;
import com.hms.patient.entity.Patient;
import com.hms.patient.entity.TreatmentCase;
import com.hms.patient.enums.CaseStatus;
import com.hms.patient.repository.ConditionMasterRepository;
import com.hms.patient.repository.PatientRepository;
import com.hms.patient.repository.TreatmentCaseRepository;
import com.hms.patient.util.PatientMapper;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TreatmentCaseService {
	private final TreatmentCaseRepository treatmentCaseRepository;
	private final PatientRepository patientRepository;
	private final ConditionMasterRepository conditionMasterRepository;

	@Transactional
	public CaseResponse create(UUID patientId, CaseRequest req) {
		Patient patient = loadPatient(patientId);
		validateDates(req.startDate(), req.expectedEndDate());

		patient.setCaseCounter(patient.getCaseCounter() + 1);
		TreatmentCase c = new TreatmentCase();
		c.setPatient(patient);
		c.setClinicId(patient.getClinicId());
		c.setCaseNumber(String.format("%s-C%02d", patient.getUhid(), patient.getCaseCounter()));
		c.setStatus(CaseStatus.PLANNED);
		c.setCompletedSessions(0);
		applyFields(c, req);

		if (req.conditions() != null) {
			req.conditions().forEach(cr -> attachCondition(c, cr));
		}
		return PatientMapper.toResponse(treatmentCaseRepository.save(c));
	}

	@Transactional(readOnly = true)
	public List<CaseResponse> list(UUID patientId) {
		loadPatient(patientId);
		return treatmentCaseRepository.findByPatientIdOrderByCreatedAtDesc(patientId).stream()
				.map(PatientMapper::toResponse).toList();
	}

	@Transactional(readOnly = true)
	public CaseResponse get(UUID patientId, UUID caseId) {
		return PatientMapper.toResponse(loadCase(patientId, caseId));
	}

	@Transactional
	public CaseResponse update(UUID patientId, UUID caseId, CaseRequest req) {
		TreatmentCase c = loadCase(patientId, caseId);
		if (c.getStatus() == CaseStatus.COMPLETED || c.getStatus() == CaseStatus.CANCELLED) {
			throw new BusinessException("A " + c.getStatus() + " case cannot be edited");
		}
		validateDates(req.startDate(), req.expectedEndDate());
		if (req.plannedSessions() < c.getCompletedSessions()) {
			throw new BusinessException(
					"Planned sessions cannot be lower than completed sessions (" + c.getCompletedSessions() + ")");
		}
		applyFields(c, req);
		return PatientMapper.toResponse(c);
	}

	@Transactional
	public CaseResponse changeStatus(UUID patientId, UUID caseId, CaseStatusRequest req) {
		TreatmentCase c = loadCase(patientId, caseId);
		CaseStatus target = req.status();
		if (!c.getStatus().canMoveTo(target)) {
			throw new BusinessException("Cannot change case from " + c.getStatus() + " to " + target);
		}
		boolean needsReason = target == CaseStatus.CANCELLED || target == CaseStatus.ON_HOLD;
		if (needsReason && (req.reason() == null || req.reason().isBlank())) {
			throw new BusinessException("A reason is required to move a case to " + target);
		}
		if (target == CaseStatus.ACTIVE && c.getStartDate() == null) {
			c.setStartDate(LocalDate.now());
		}
		if (target == CaseStatus.COMPLETED || target == CaseStatus.CANCELLED) {
			c.setClosedOn(LocalDate.now());
			c.setClosureReason(req.reason());
		}
		c.setStatus(target);
		return PatientMapper.toResponse(c);
	}

	/**
	 * Called when a session is delivered (later also by appointment-service
	 * events).
	 */
	@Transactional
	public CaseResponse recordSessionCompleted(UUID patientId, UUID caseId) {
		TreatmentCase c = loadCase(patientId, caseId);
		if (c.getStatus() != CaseStatus.ACTIVE) {
			throw new BusinessException("Sessions can only be recorded on an ACTIVE case");
		}
		if (c.getCompletedSessions() >= c.getPlannedSessions()) {
			throw new BusinessException("All planned sessions are completed. Increase planned sessions first.");
		}
		c.setCompletedSessions(c.getCompletedSessions() + 1);
		return PatientMapper.toResponse(c);
	}

	@Transactional
	public void delete(UUID patientId, UUID caseId) {
		loadCase(patientId, caseId).softDelete(SecurityUtils.currentUsername());
	}

	@Transactional
	public CaseResponse addCondition(UUID patientId, UUID caseId, ConditionRequest req) {
		TreatmentCase c = loadCase(patientId, caseId);
		attachCondition(c, req);
		return PatientMapper.toResponse(c);
	}

	@Transactional
	public CaseResponse removeCondition(UUID patientId, UUID caseId, UUID conditionId) {
		TreatmentCase c = loadCase(patientId, caseId);
		CaseCondition cc = c.getConditions().stream().filter(x -> x.getId().equals(conditionId)).findFirst()
				.orElseThrow(() -> new ResourceNotFoundException("Condition", conditionId));
		cc.setActive(false);
		cc.softDelete(SecurityUtils.currentUsername());
		c.getConditions().remove(cc);
		return PatientMapper.toResponse(c);
	}

	// Helper Methods
	private Patient loadPatient(UUID patientId) {
		Patient p = patientRepository.findById(patientId)
				.orElseThrow(() -> new ResourceNotFoundException("Patient", patientId));
		ClinicScope.assertSameClinic(p.getClinicId(), "Patient", patientId);
		return p;
	}

	private void validateDates(LocalDate start, LocalDate end) {
		if (start != null && end != null && end.isBefore(start)) {
			throw new BusinessException("Expected end date cannot be before start date");
		}
	}

	private void applyFields(TreatmentCase c, CaseRequest r) {
		c.setDiagnosis(r.diagnosis().trim());
		c.setIcdCode(r.icdCode());
		c.setBodyRegion(r.bodyRegion());
		c.setLaterality(r.laterality());
		c.setPrimaryTherapistId(r.primaryTherapistId());
		c.setPlannedSessions(r.plannedSessions());
		c.setStartDate(r.startDate());
		c.setExpectedEndDate(r.expectedEndDate());
		c.setNotes(r.notes());
	}

	private void attachCondition(TreatmentCase c, ConditionRequest req) {
		ConditionMaster master = conditionMasterRepository.findByCodeIgnoreCase(req.conditionCode())
				.filter(ConditionMaster::isActive)
				.orElseThrow(() -> new BusinessException("Unknown or inactive condition: " + req.conditionCode()));
		boolean exists = c.getConditions().stream()
				.anyMatch(x -> x.getConditionCode().equalsIgnoreCase(master.getCode()));
		if (exists) {
			throw new DuplicateResourceException("Condition already added to this case: " + master.getCode());
		}
		CaseCondition cc = new CaseCondition();
		cc.setTreatmentCase(c);
		cc.setClinicId(c.getClinicId());
		cc.setConditionCode(master.getCode());
		cc.setConditionName(master.getName());
		cc.setNotes(req.notes());
		cc.setDiagnosedOn(req.diagnosedOn());
		c.getConditions().add(cc);
	}

	private TreatmentCase loadCase(UUID patientId, UUID caseId) {
		TreatmentCase c = treatmentCaseRepository.findByIdAndPatientId(caseId, patientId)
				.orElseThrow(() -> new ResourceNotFoundException("Treatment case", caseId));
		ClinicScope.assertSameClinic(c.getClinicId(), "Treatment case", caseId);
		return c;
	}
}
