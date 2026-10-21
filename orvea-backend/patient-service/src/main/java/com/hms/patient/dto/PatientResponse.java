package com.hms.patient.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import com.hms.common.enums.Gender;
import com.hms.patient.enums.PatientStatus;

public record PatientResponse(UUID id, String uhid, String firstName, String lastName, String fullName, Gender gender,
		LocalDate dateOfBirth, int age, boolean dobApproximate, String bloodGroup, String countryCode, String phone,
		String alternatePhone, String email, String addressLine, String city, String state, String pincode,
		String occupation, String referredBy, String emergencyContactName, String emergencyContactPhone,
		String emergencyContactRelation, UUID userId, PatientStatus status, LocalDate registeredOn, String notes,
		LocalDateTime createdAt) {

}
