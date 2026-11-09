package com.hms.notification.dto;

import com.hms.notification.enums.WhatsAppProviderType;

public record MessagingSettingsResponse(boolean enabled, WhatsAppProviderType provider, boolean sendBookingConfirmation,
		boolean sendReminders, boolean sendCancellation, boolean sendReschedule, boolean requireConsent,
		String clinicName, String clinicPhone, String defaultCountryCode, String cloudPhoneNumberId,
		boolean cloudTokenConfigured) {
}
