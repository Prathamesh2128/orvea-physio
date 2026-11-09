package com.hms.billing.entity;

import java.time.LocalDate;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.type.SqlTypes;

import com.hms.common.entity.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * One session taken from a package. A mistake is reversed (kept with the
 * reason), not deleted.
 */
@SuppressWarnings("serial")
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "package_usages", indexes = { @Index(name = "idx_usage_package", columnList = "package_id"),
		@Index(name = "idx_usage_appointment", columnList = "appointment_id") })
@SQLRestriction("is_deleted = false")
public class PackageUsage extends BaseEntity {

	@JdbcTypeCode(SqlTypes.CHAR)
	@Column(name = "package_id", columnDefinition = "CHAR(36)", nullable = false)
	private UUID packageId;

	@JdbcTypeCode(SqlTypes.CHAR)
	@Column(name = "appointment_id", columnDefinition = "CHAR(36)")
	private UUID appointmentId;

	@Column(name = "used_on", nullable = false)
	private LocalDate usedOn;

	/** AUTO (from a completed visit) or MANUAL. */
	@Column(nullable = false, length = 10)
	private String source;

	@Column(length = 255)
	private String note;

	@Column(nullable = false)
	private boolean reversed = false;
	@Column(name = "reverse_reason", length = 255)
	private String reverseReason;
}