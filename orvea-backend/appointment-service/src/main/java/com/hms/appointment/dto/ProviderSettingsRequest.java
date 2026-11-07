package com.hms.appointment.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record ProviderSettingsRequest(Integer slotMinutes, @Min(1) @Max(10) Integer maxParallel, Boolean active) {
}