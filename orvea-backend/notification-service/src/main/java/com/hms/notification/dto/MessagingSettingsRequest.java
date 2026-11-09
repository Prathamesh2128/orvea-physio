package com.hms.notification.dto;

import com.hms.notification.enums.WhatsAppProviderType;

import jakarta.validation.constraints.Size;

/**
 * All fields optional: only what is sent is changed. cloudAccessToken is
 * write-only.
 */
public record MessagingSettingsRequest(Boolean enabled, WhatsAppProviderType provider, Boolean sendBookingConfirmation,
		Boolean sendReminders, Boolean sendCancellation, Boolean sendReschedule, Boolean requireConsent,
		@Size(max = 120) String clinicName, @Size(max = 30) String clinicPhone,
		@Size(max = 5) String defaultCountryCode, @Size(max = 40) String cloudPhoneNumberId,
		@Size(max = 500) String cloudAccessToken) {
}
