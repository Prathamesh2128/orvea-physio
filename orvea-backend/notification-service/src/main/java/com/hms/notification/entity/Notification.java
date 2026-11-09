package com.hms.notification.entity;

import java.time.LocalDateTime;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.type.SqlTypes;

import com.hms.common.entity.BaseEntity;
import com.hms.notification.enums.MessageType;
import com.hms.notification.enums.NotificationStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@SuppressWarnings("serial")
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "notifications", uniqueConstraints = @UniqueConstraint(name = "uk_notification_dedupe", columnNames = "dedupe_key"), indexes = {
		@Index(name = "idx_notif_status", columnList = "status,created_at"),
		@Index(name = "idx_notif_appt", columnList = "appointment_id") })
@SQLRestriction("is_deleted = false")
public class Notification extends BaseEntity {

	/** Makes consumers idempotent: the same event can never create two messages. */
	@Column(name = "dedupe_key", nullable = false, length = 100)
	private String dedupeKey;

	@Enumerated(EnumType.STRING)
	@Column(name = "message_type", nullable = false, length = 25)
	private MessageType messageType;

	@Column(nullable = false, length = 15)
	private String channel = "WHATSAPP";

	@Column(length = 20)
	private String provider;

	@JdbcTypeCode(SqlTypes.CHAR)
	@Column(name = "patient_id", columnDefinition = "CHAR(36)")
	private UUID patientId;

	@JdbcTypeCode(SqlTypes.CHAR)
	@Column(name = "appointment_id", columnDefinition = "CHAR(36)")
	private UUID appointmentId;

	@Column(name = "appointment_at")
	private LocalDateTime appointmentAt;

	@Column(name = "recipient_name", length = 150)
	private String recipientName;

	/** Digits with country code, e.g. 919876543210. */
	@Column(name = "recipient_phone", length = 20)
	private String recipientPhone;

	@Column(length = 1000)
	private String body;

	/**
	 * Placeholder values as JSON; CLOUD_API turns them into template parameters.
	 */
	@Column(name = "template_vars", length = 1000)
	private String templateVars;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 15)
	private NotificationStatus status = NotificationStatus.PENDING;

	@Column(name = "status_reason", length = 255)
	private String statusReason;

	/** MANUAL_LINK: https://wa.me/<phone>?text=<message> */
	@Column(name = "wa_link", length = 2000)
	private String waLink;

	@Column(name = "provider_message_id", length = 100)
	private String providerMessageId;

	@Column(nullable = false)
	private int attempts = 0;

	@Column(name = "last_error", length = 255)
	private String lastError;

	@Column(name = "sent_at")
	private LocalDateTime sentAt;

	@Column(name = "sent_by", length = 100)
	private String sentBy;
}
