package com.hms.patient.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record WithdrawConsentRequest(@NotBlank @Size(max = 255) String reason) {}
