package com.hms.assessment.dto;

import java.util.Map;

import com.hms.assessment.enums.CaptureKind;
import com.hms.assessment.enums.CaptureView;
import com.hms.common.enums.BodyRegion;
import com.hms.common.enums.Laterality;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CaptureRequest(@NotNull CaptureKind kind, CaptureView view, BodyRegion bodyRegion, Laterality laterality,
		@NotEmpty Map<String, Double> angles, @DecimalMin("0.0") @DecimalMax("1.0") Double qualityScore,
		@Size(max = 60) String toolName, @Size(max = 40) String toolVersion, @Size(max = 120) String questionRef,
		@Size(max = 500) String note) {
}