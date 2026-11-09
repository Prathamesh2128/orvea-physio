package com.hms.assessment.dto;

public record ValidateTemplateResponse(boolean valid, String error, int questionCount) {
}