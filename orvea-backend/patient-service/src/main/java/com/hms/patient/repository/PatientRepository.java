package com.hms.patient.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.hms.patient.entity.Patient;

public interface PatientRepository extends JpaRepository<Patient, UUID> {

}
