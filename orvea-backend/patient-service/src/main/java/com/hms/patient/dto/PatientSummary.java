package com.hms.patient.dto;

import java.time.LocalDate;
import java.util.UUID;

import com.hms.common.enums.Gender;
import com.hms.patient.enums.PatientStatus;

public record PatientSummary(UUID id, String uhid, String fullName, Gender gender, int age, String phone,
		PatientStatus status, LocalDate registeredOn) {

}
