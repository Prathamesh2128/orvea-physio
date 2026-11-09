package com.hms.assessment.dto;

import java.util.Map;

import jakarta.validation.constraints.Size;

/**
 * confirmedAngles: the clinician's final values (omit to accept the
 * suggestion). angleKey: which angle fills the linked question.
 */
public record ConfirmCaptureRequest(Map<String, Double> confirmedAngles, @Size(max = 60) String angleKey,
		@Size(max = 500) String note) {
}