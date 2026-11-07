package com.hms.staff.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import com.hms.common.enums.Gender;
import com.hms.common.enums.RoleType;
import com.hms.staff.enums.StaffStatus;

public record StaffResponse(UUID id, UUID userId, String firstName, String middleName, String lastName,
		String displayName, Gender gender, RoleType staffType, String phone, String email, String qualification,
		String registrationNumber, String specialization, Integer experienceYears, LocalDate joiningDate,
		StaffStatus status, boolean bookable, LocalDateTime createdAt) {

}
