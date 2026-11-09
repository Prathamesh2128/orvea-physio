package com.hms.assessment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * code is only used on create (a condition code, or COMMON). definition is the
 * template JSON object.
 */
public record TemplateRequest(
		@Pattern(regexp = "^[A-Za-z][A-Za-z0-9_]{0,59}$", message = "Letters, digits and underscore only") String code,
		@NotBlank @Size(max = 120) String name, @Size(max = 500) String description, @NotNull Object definition) {
}