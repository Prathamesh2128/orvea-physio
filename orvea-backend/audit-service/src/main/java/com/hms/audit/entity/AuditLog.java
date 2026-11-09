package com.hms.audit.entity;

import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Immutable record: no update or delete endpoints, no soft-delete flag. */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "audit_logs", uniqueConstraints = @UniqueConstraint(name = "uk_audit_event", columnNames = "event_id"), indexes = {
		@Index(name = "idx_audit_entity", columnList = "entity_type,entity_id"),
		@Index(name = "idx_audit_actor", columnList = "actor"),
		@Index(name = "idx_audit_clinic_time", columnList = "clinic_id,occurred_at") })
public class AuditLog {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	@JdbcTypeCode(SqlTypes.CHAR)
	@Column(name = "id", columnDefinition = "CHAR(36)", updatable = false, nullable = false)
	private UUID id;

	/** Makes ingestion idempotent. */
	@Column(name = "event_id", nullable = false, length = 50, updatable = false)
	private String eventId;

	@JdbcTypeCode(SqlTypes.CHAR)
	@Column(name = "clinic_id", columnDefinition = "CHAR(36)", updatable = false)
	private UUID clinicId;

	@Column(name = "source_service", length = 40, updatable = false)
	private String sourceService;

	@Column(nullable = false, length = 60, updatable = false)
	private String action;

	@Column(name = "entity_type", nullable = false, length = 60, updatable = false)
	private String entityType;

	@Column(name = "entity_id", length = 64, updatable = false)
	private String entityId;

	@Column(nullable = false, length = 100, updatable = false)
	private String actor;

	@Column(name = "occurred_at", nullable = false, updatable = false)
	private Instant occurredAt;

	/** JSON text. Ids and codes only, no personal data. */
	@Column(columnDefinition = "LONGTEXT", updatable = false)
	private String details;

	@Column(name = "received_at", nullable = false, updatable = false)
	private Instant receivedAt;
}
