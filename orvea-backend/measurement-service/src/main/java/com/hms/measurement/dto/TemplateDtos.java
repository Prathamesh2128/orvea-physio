package com.hms.measurement.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import com.hms.measurement.enums.Landmark;
import com.hms.measurement.enums.LimbType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public final class TemplateDtos {
	private TemplateDtos() {
	}

	/**
	 * shared=true makes it a clinic preset (needs MEASUREMENT_SETTINGS); otherwise
	 * it is the therapist's own.
	 */
	public record TemplateRequest(@NotBlank @Size(max = 120) String name, @NotNull LimbType limbType,
			@NotNull BigDecimal stepCm, @NotNull Landmark startLandmark, @NotNull Integer pointCount, Boolean shared) {
	}

	public record TemplateResponse(UUID id, String name, LimbType limbType, BigDecimal stepCm, Landmark startLandmark,
			int pointCount, List<BigDecimal> plannedDistancesCm, boolean shared, boolean mine, String description) {
	}
}