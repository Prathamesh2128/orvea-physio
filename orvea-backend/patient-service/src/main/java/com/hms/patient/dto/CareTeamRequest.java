package com.hms.patient.dto;

import java.util.UUID;

import com.hms.patient.enums.CareTeamRole;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CareTeamRequest(UUID caseId, @NotNull CareTeamRole role, @NotBlank @Size(max = 120) String name,
		@Size(max = 20) String phone, @Email String email, @Size(max = 150) String organization,
		@Size(max = 60) String relationship, boolean primaryContact, @Size(max = 500) String notes) { }
