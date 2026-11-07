package com.hms.staff.dto;

import java.time.LocalDate;
import java.util.UUID;

import com.hms.common.enums.Gender;
import com.hms.common.enums.RoleType;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record StaffRequest(@NotBlank @Size(max = 60) String firstName, @Size(max = 60) String middleName,
		@NotBlank @Size(max = 60) String lastName, Gender gender, @NotNull RoleType staffType,
		@Size(max = 20) String phone, @Email @Size(max = 150) String email, @Size(max = 150) String qualification,
		@Size(max = 60) String registrationNumber, @Size(max = 150) String specialization,
		@Min(0) @Max(70) Integer experienceYears, LocalDate joiningDate, UUID userId) {

}
