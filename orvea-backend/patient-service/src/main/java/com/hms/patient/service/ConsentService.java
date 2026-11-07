package com.hms.patient.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hms.common.exception.BusinessException;
import com.hms.common.exception.DuplicateResourceException;
import com.hms.common.exception.ResourceNotFoundException;
import com.hms.common.security.ClinicScope;
import com.hms.patient.dto.ConsentRequest;
import com.hms.patient.dto.ConsentResponse;
import com.hms.patient.dto.WithdrawConsentRequest;
import com.hms.patient.entity.Patient;
import com.hms.patient.entity.PatientConsent;
import com.hms.patient.enums.ConsentStatus;
import com.hms.patient.enums.ConsentType;
import com.hms.patient.repository.PatientConsentRepository;
import com.hms.patient.repository.PatientRepository;
import com.hms.patient.util.PatientMapper;

import lombok.RequiredArgsConstructor;

/**
 * Consent rows are an audit trail: withdrawing keeps the row; granting again
 * creates a new row.
 */
@Service
@RequiredArgsConstructor
public class ConsentService {
	private final PatientConsentRepository consentRepository;
	private final PatientRepository patientRepository;

	@Transactional
	public ConsentResponse grant(UUID patientId, ConsentRequest req) {
		Patient patient = loadPatient(patientId);
		if (consentRepository.existsByPatientIdAndConsentTypeAndStatus(patientId, req.consentType(),
				ConsentStatus.GRANTED)) {
			throw new DuplicateResourceException("Consent already granted for " + req.consentType());
		}
		PatientConsent c = new PatientConsent();
		c.setPatient(patient);
		c.setClinicId(patient.getClinicId());
		c.setConsentType(req.consentType());
		c.setGivenBy(req.givenBy());
		c.setSignerName(req.signerName());
		c.setConsentVersion(req.consentVersion());
		c.setDocumentRef(req.documentRef());
		c.setNotes(req.notes());
		c.setStatus(ConsentStatus.GRANTED);
		c.setGrantedAt(LocalDateTime.now());
		return PatientMapper.toResponse(consentRepository.save(c));
	}

	@Transactional
	public ConsentResponse withdraw(UUID patientId, UUID consentId, WithdrawConsentRequest req) {
		loadPatient(patientId);
		PatientConsent c = consentRepository.findByIdAndPatientId(consentId, patientId)
				.orElseThrow(() -> new ResourceNotFoundException("Consent", consentId));
		if (c.getStatus() == ConsentStatus.WITHDRAWN) {
			throw new BusinessException("Consent is already withdrawn");
		}
		c.setStatus(ConsentStatus.WITHDRAWN);
		c.setWithdrawnAt(LocalDateTime.now());
		c.setWithdrawalReason(req.reason());
		return PatientMapper.toResponse(c);
	}

	@Transactional(readOnly = true)
	public List<ConsentResponse> list(UUID patientId) {
		loadPatient(patientId);
		return consentRepository.findByPatientIdOrderByCreatedAtDesc(patientId).stream().map(PatientMapper::toResponse)
				.toList();
	}

	/**
	 * Used by other services (reminders, camera analysis, photos) before acting.
	 */
	@Transactional(readOnly = true)
	public boolean hasActiveConsent(UUID patientId, ConsentType type) {
		loadPatient(patientId);
		return consentRepository.existsByPatientIdAndConsentTypeAndStatus(patientId, type, ConsentStatus.GRANTED);
	}

	private Patient loadPatient(UUID patientId) {
		Patient p = patientRepository.findById(patientId)
				.orElseThrow(() -> new ResourceNotFoundException("Patient", patientId));
		ClinicScope.assertSameClinic(p.getClinicId(), "Patient", patientId);
		return p;
	}
}
