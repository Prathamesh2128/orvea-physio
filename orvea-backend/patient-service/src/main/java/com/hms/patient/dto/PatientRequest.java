package com.hms.patient.dto;

import java.time.LocalDate;
import java.util.UUID;

import com.hms.common.enums.Gender;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;

public record PatientRequest(@NotBlank @Size(max = 60) String firstName,
        @Size(max = 60) String lastName,
        @NotNull Gender gender,
        @NotNull @Past LocalDate dateOfBirth,
        boolean dobApproximate,
        @Size(max = 5) String bloodGroup,
        @Size(max = 5) String countryCode,
        @NotBlank @Size(max = 20) String phone,
        @Size(max = 20) String alternatePhone,
        @Email @Size(max = 150) String email,
        @Size(max = 255) String addressLine,
        @Size(max = 80) String city,
        @Size(max = 80) String state,
        @Size(max = 12) String pincode,
        @Size(max = 100) String occupation,
        @Size(max = 150) String referredBy,
        @Size(max = 100) String emergencyContactName,
        @Size(max = 20) String emergencyContactPhone,
        @Size(max = 50) String emergencyContactRelation,
        UUID userId,
        @Size(max = 1000) String notes,
        Boolean confirmDuplicate) { }
