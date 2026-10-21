package com.hms.patient.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.hms.patient.entity.TreatmentCase;

public interface TreatmentCaseRepository extends JpaRepository<TreatmentCase, UUID> {

}
