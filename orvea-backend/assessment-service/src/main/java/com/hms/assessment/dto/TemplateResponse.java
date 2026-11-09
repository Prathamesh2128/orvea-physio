package com.hms.assessment.dto;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

import com.hms.assessment.enums.TemplateStatus;

public record TemplateResponse(UUID id, String code, String name, String description, int version,
		TemplateStatus status, boolean sharedDefault, Map<String, Object> definition, LocalDateTime publishedAt,
		LocalDateTime createdAt) {
}