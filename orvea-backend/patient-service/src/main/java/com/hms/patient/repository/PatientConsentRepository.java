package com.hms.patient.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.hms.patient.entity.PatientConsent;
import com.hms.patient.enums.ConsentStatus;
import com.hms.patient.enums.ConsentType;

public interface PatientConsentRepository extends JpaRepository<PatientConsent, UUID> {
	List<PatientConsent> findByPatientIdOrderByCreatedAtDesc(UUID patientId);

	Optional<PatientConsent> findByIdAndPatientId(UUID id, UUID patientId);

	boolean existsByPatientIdAndConsentTypeAndStatus(UUID patientId, ConsentType type, ConsentStatus status);

}
