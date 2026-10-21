package com.hms.patient.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.hms.patient.entity.PatientConsent;

public interface PatientConsentRepository extends JpaRepository<PatientConsent, UUID>{

}
