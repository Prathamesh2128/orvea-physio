package com.hms.assessment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RejectCaptureRequest(@NotBlank @Size(max = 255) String reason) {
}