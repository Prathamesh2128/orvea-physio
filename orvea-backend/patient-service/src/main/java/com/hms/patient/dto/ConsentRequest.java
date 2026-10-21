package com.hms.patient.dto;

import com.hms.patient.enums.ConsentGivenBy;
import com.hms.patient.enums.ConsentType;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ConsentRequest(@NotNull ConsentType consentType, @NotNull ConsentGivenBy givenBy,
        @Size(max = 120) String signerName, @Size(max = 20) String consentVersion,
        @Size(max = 255) String documentRef, @Size(max = 500) String notes) {}
