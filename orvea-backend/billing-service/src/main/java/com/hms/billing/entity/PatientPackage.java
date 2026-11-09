package com.hms.billing.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.type.SqlTypes;

import com.hms.billing.enums.PackageStatus;
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

/**
 * A package a patient bought. Name, sessions and validity are copied at sale,
 * so later template edits never change it.
 */
@SuppressWarnings("serial")
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "patient_packages", indexes = { @Index(name = "idx_pkg_patient", columnList = "patient_id,status"),
		@Index(name = "idx_pkg_invoice", columnList = "invoice_id") })
@SQLRestriction("is_deleted = false")
public class PatientPackage extends BaseEntity {

	@JdbcTypeCode(SqlTypes.CHAR)
	@Column(name = "patient_id", columnDefinition = "CHAR(36)", nullable = false)
	private UUID patientId;
	@Column(name = "patient_name", length = 150)
	private String patientName;
	@Column(name = "patient_uhid", length = 30)
	private String patientUhid;

	@JdbcTypeCode(SqlTypes.CHAR)
	@Column(name = "case_id", columnDefinition = "CHAR(36)")
	private UUID caseId;

	@JdbcTypeCode(SqlTypes.CHAR)
	@Column(name = "template_id", columnDefinition = "CHAR(36)")
	private UUID templateId;
	@Column(nullable = false, length = 120)
	private String name;

	@JdbcTypeCode(SqlTypes.CHAR)
	@Column(name = "service_id", columnDefinition = "CHAR(36)", nullable = false)
	private UUID serviceId;
	@Column(name = "service_name", length = 120)
	private String serviceName;

	@Column(name = "sessions_total", nullable = false)
	private int sessionsTotal;
	@Column(name = "sessions_used", nullable = false)
	private int sessionsUsed = 0;

	@Column(nullable = false, precision = 12, scale = 2)
	private BigDecimal price;

	@Column(name = "purchased_on", nullable = false)
	private LocalDate purchasedOn;
	@Column(name = "valid_until", nullable = false)
	private LocalDate validUntil;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 10)
	private PackageStatus status = PackageStatus.ACTIVE;

	@JdbcTypeCode(SqlTypes.CHAR)
	@Column(name = "invoice_id", columnDefinition = "CHAR(36)")
	private UUID invoiceId;

	@Column(name = "cancel_reason", length = 255)
	private String cancelReason;
}