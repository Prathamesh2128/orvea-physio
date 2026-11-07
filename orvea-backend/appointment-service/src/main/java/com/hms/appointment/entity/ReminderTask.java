package com.hms.appointment.entity;

import java.time.LocalDateTime;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.type.SqlTypes;

import com.hms.appointment.enums.ReminderStatus;
import com.hms.common.entity.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@SuppressWarnings("serial")
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "reminder_tasks", indexes = { @Index(name = "idx_reminder_due", columnList = "status,remind_at"),
		@Index(name = "idx_reminder_appt", columnList = "appointment_id") })
@SQLRestriction("is_deleted = false")
public class ReminderTask extends BaseEntity {

	@JdbcTypeCode(SqlTypes.CHAR)
	@Column(name = "appointment_id", columnDefinition = "CHAR(36)", nullable = false)
	private UUID appointmentId;

	@Column(name = "remind_at", nullable = false)
	private LocalDateTime remindAt;

	@Column(name = "offset_minutes", nullable = false)
	private int offsetMinutes;

	/** Ordered preference, e.g. "WHATSAPP,SMS" (second is the fallback). */
	@Column(nullable = false, length = 60)
	private String channels;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 12)
	private ReminderStatus status = ReminderStatus.PENDING;

	@Column(name = "dispatched_at")
	private LocalDateTime dispatchedAt;

	@Column(nullable = false)
	private int attempts = 0;

	@Column(name = "last_error", length = 255)
	private String lastError;
}
