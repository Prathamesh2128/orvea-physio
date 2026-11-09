package com.hms.assessment.dto;

import jakarta.validation.constraints.NotNull;

public record ValidateTemplateRequest(@NotNull Object definition) {
}