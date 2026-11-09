package com.hms.assessment.dto;

import jakarta.validation.constraints.NotNull;

import java.util.Map;

/**
 * Only the answers that changed: {"KNEE": {"swelling": true}}. A null value
 * clears an answer.
 */
public record SaveAnswersRequest(@NotNull Map<String, Map<String, Object>> answers) {
}