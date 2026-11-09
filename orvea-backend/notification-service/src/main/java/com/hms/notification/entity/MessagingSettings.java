package com.hms.notification.entity;

import org.hibernate.annotations.SQLRestriction;

import com.hms.common.entity.BaseEntity;
import com.hms.notification.enums.WhatsAppProviderType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@SuppressWarnings("serial")
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "messaging_settings")
@SQLRestriction("is_deleted = false")
public class MessagingSettings extends BaseEntity {

	/**
	 * Master switch. OFF by default so a new clinic never messages anyone by
	 * accident.
	 */
	@Column(nullable = false)
	private boolean enabled = false;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private WhatsAppProviderType provider = WhatsAppProviderType.MANUAL_LINK;

	@Column(name = "send_booking_confirmation", nullable = false)
	private boolean sendBookingConfirmation = true;
	@Column(name = "send_reminders", nullable = false)
	private boolean sendReminders = true;
	@Column(name = "send_cancellation", nullable = false)
	private boolean sendCancellation = true;
	@Column(name = "send_reschedule", nullable = false)
	private boolean sendReschedule = true;

	/**
	 * Only message patients who consented (patient-service consent
	 * REMINDER_SMS_WHATSAPP).
	 */
	@Column(name = "require_consent", nullable = false)
	private boolean requireConsent = true;

	@Column(name = "clinic_name", length = 120)
	private String clinicName = "Our Clinic";

	@Column(name = "clinic_phone", length = 30)
	private String clinicPhone;

	/** Digits only, e.g. 91. Used for 10-digit numbers. */
	@Column(name = "default_country_code", nullable = false, length = 5)
	private String defaultCountryCode = "91";

	@Column(name = "cloud_phone_number_id", length = 40)
	private String cloudPhoneNumberId;

	/** AES-GCM encrypted. Never returned by the API. */
	@Column(name = "cloud_access_token_enc", length = 1000)
	private String cloudAccessTokenEnc;
}
