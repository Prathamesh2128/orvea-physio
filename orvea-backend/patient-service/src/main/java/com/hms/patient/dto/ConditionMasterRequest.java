package com.hms.patient.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ConditionMasterRequest(
		@NotBlank @Size(max = 50) @Pattern(regexp = "^[A-Za-z0-9_]+$", message = "Only letters, digits and underscore") String code,
		@NotBlank @Size(max = 120) String name, @Size(max = 40) String category, @Size(max = 500) String description,
		Boolean active, Integer sortOrder) {

}
