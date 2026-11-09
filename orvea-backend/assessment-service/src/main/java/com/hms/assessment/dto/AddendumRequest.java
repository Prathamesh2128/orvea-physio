package com.hms.assessment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AddendumRequest(@NotBlank @Size(max = 2000) String text) {
}