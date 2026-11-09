package com.hms.notification.service;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hms.common.audit.AuditPublisher;
import com.hms.common.exception.BusinessException;
import com.hms.common.security.ClinicScope;
import com.hms.notification.dto.MessagingSettingsRequest;
import com.hms.notification.dto.MessagingSettingsResponse;
import com.hms.notification.entity.MessagingSettings;
import com.hms.notification.enums.WhatsAppProviderType;
import com.hms.notification.repository.MessagingSettingsRepository;
import com.hms.notification.security.SecretCipher;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MessagingSettingsService {

	private final MessagingSettingsRepository repository;
	private final SecretCipher cipher;
	private final AuditPublisher audit;

	/** Get-or-create. Defaults: messaging OFF, MANUAL_LINK (free). */
	@Transactional
	public MessagingSettings effective(UUID clinicId) {
		return repository.findByClinicId(clinicId).orElseGet(() -> {
			MessagingSettings s = new MessagingSettings();
			s.setClinicId(clinicId);
			return repository.save(s);
		});
	}

	@Transactional
	public MessagingSettingsResponse get() {
		return toResponse(effective(ClinicScope.currentClinicId()));
	}

	@Transactional
	public MessagingSettingsResponse update(MessagingSettingsRequest r) {
		MessagingSettings s = effective(ClinicScope.currentClinicId());
		Map<String, Object> changed = new LinkedHashMap<>();

		if (r.enabled() != null) {
			s.setEnabled(r.enabled());
			changed.put("enabled", r.enabled());
		}
		if (r.provider() != null) {
			s.setProvider(r.provider());
			changed.put("provider", r.provider().name());
		}
		if (r.sendBookingConfirmation() != null)
			s.setSendBookingConfirmation(r.sendBookingConfirmation());
		if (r.sendReminders() != null)
			s.setSendReminders(r.sendReminders());
		if (r.sendCancellation() != null)
			s.setSendCancellation(r.sendCancellation());
		if (r.sendReschedule() != null)
			s.setSendReschedule(r.sendReschedule());
		if (r.requireConsent() != null) {
			s.setRequireConsent(r.requireConsent());
			changed.put("requireConsent", r.requireConsent());
		}
		if (r.clinicName() != null)
			s.setClinicName(r.clinicName().isBlank() ? null : r.clinicName().trim());
		if (r.clinicPhone() != null)
			s.setClinicPhone(r.clinicPhone().isBlank() ? null : r.clinicPhone().trim());
		if (r.defaultCountryCode() != null && !r.defaultCountryCode().isBlank()) {
			s.setDefaultCountryCode(r.defaultCountryCode().replaceAll("\\D", ""));
		}
		if (r.cloudPhoneNumberId() != null)
			s.setCloudPhoneNumberId(r.cloudPhoneNumberId().isBlank() ? null : r.cloudPhoneNumberId().trim());
		if (r.cloudAccessToken() != null && !r.cloudAccessToken().isBlank()) {
			s.setCloudAccessTokenEnc(cipher.encrypt(r.cloudAccessToken().trim())); // blank = keep the stored token
			changed.put("cloudTokenUpdated", true);
		}

		if (s.isEnabled()) {
			if (s.getClinicName() == null)
				throw new BusinessException("Set the clinic name before switching messaging on");
			if (s.getProvider() == WhatsAppProviderType.CLOUD_API
					&& (s.getCloudPhoneNumberId() == null || s.getCloudAccessTokenEnc() == null)) {
				throw new BusinessException("CLOUD_API needs the WhatsApp phone number id and an access token");
			}
		}
		audit.record("MESSAGING_SETTINGS_CHANGED", "MessagingSettings", s.getId(), changed); // never contains the token
		return toResponse(s);
	}

	private MessagingSettingsResponse toResponse(MessagingSettings s) {
		return new MessagingSettingsResponse(s.isEnabled(), s.getProvider(), s.isSendBookingConfirmation(),
				s.isSendReminders(), s.isSendCancellation(), s.isSendReschedule(), s.isRequireConsent(),
				s.getClinicName(), s.getClinicPhone(), s.getDefaultCountryCode(), s.getCloudPhoneNumberId(),
				s.getCloudAccessTokenEnc() != null);
	}
}
