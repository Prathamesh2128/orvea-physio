package com.hms.appointment.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record VisitTypeRequest(@NotBlank @Size(max = 40) @Pattern(regexp = "^[A-Za-z0-9_]+$") String code,
		@NotBlank @Size(max = 100) String name, @Min(5) @Max(240) int durationMinutes, boolean countsAsSession,
		Boolean active, Integer sortOrder) {
}
