package com.hms.assessment.dto;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

import com.hms.assessment.enums.CaptureKind;
import com.hms.assessment.enums.CaptureStatus;
import com.hms.assessment.enums.CaptureView;
import com.hms.common.enums.BodyRegion;
import com.hms.common.enums.Laterality;

public record CaptureResponse(UUID id, CaptureKind kind, CaptureView view, BodyRegion bodyRegion, Laterality laterality,
		Map<String, Object> angles, Map<String, Object> confirmedAngles, Double qualityScore, String toolName,
		String toolVersion, String questionRef, CaptureStatus status, String note, boolean hasImage, String confirmedBy,
		LocalDateTime confirmedAt, LocalDateTime createdAt) {
}