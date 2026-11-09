package com.hms.measurement.entity;

import java.time.LocalDate;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.type.SqlTypes;

import com.hms.common.entity.BaseEntity;
import com.hms.measurement.enums.PlanEventType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * History of what happened to a plan: measured, snoozed, skipped (with
 * reasons), protocol changes...
 */
@SuppressWarnings("serial")
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "plan_events", indexes = @Index(name = "idx_event_plan", columnList = "plan_id,created_at"))
@SQLRestriction("is_deleted = false")
public class PlanEvent extends BaseEntity {

	@JdbcTypeCode(SqlTypes.CHAR)
	@Column(name = "plan_id", columnDefinition = "CHAR(36)", nullable = false)
	private UUID planId;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 25)
	private PlanEventType type;

	@Column(name = "event_date", nullable = false)
	private LocalDate eventDate;

	@Column(length = 255)
	private String reason;

	@Column(columnDefinition = "JSON")
	private String details;

	@Column(nullable = false, length = 100)
	private String actor;
}