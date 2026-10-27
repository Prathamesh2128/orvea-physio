package com.hms.patient.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.hms.patient.entity.TreatmentCase;

public interface TreatmentCaseRepository extends JpaRepository<TreatmentCase, UUID> {

	List<TreatmentCase> findByPatientIdOrderByCreatedAtDesc(UUID patientId);

	List<TreatmentCase> findByPatientId(UUID patientId);

	Optional<TreatmentCase> findByIdAndPatientId(UUID id, UUID patientId);
}
