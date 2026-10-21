package com.hms.patient.dto;

import java.util.UUID;

public record ConditionMasterResponse(UUID id, String code, String name, String category, String description,
		boolean active, int sortOrder) {

}
