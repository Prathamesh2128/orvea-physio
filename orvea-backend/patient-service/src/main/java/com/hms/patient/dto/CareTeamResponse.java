package com.hms.patient.dto;

import java.util.UUID;

import com.hms.patient.enums.CareTeamRole;

public record CareTeamResponse(UUID id, UUID caseId, CareTeamRole role, String name, String phone, String email,
		String organization, String relationship, boolean primaryContact, String notes) { }
