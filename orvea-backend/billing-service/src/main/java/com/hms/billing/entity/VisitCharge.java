package com.hms.billing.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.type.SqlTypes;

import com.hms.billing.enums.ChargeStatus;
import com.hms.common.entity.BaseEntity;

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

/**
 * A delivered visit waiting to be billed (created when appointment-service
 * reports it completed).
 */
@SuppressWarnings("serial")
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "visit_charges", uniqueConstraints = @UniqueConstraint(name = "uk_charge_appointment", columnNames = "appointment_id"), indexes = {
		@Index(name = "idx_charge_patient", columnList = "patient_id,status"),
		@Index(name = "idx_charge_invoice", columnList = "invoice_id") })
@SQLRestriction("is_deleted = false")
public class VisitCharge extends BaseEntity {

	@JdbcTypeCode(SqlTypes.CHAR)
	@Column(name = "appointment_id", columnDefinition = "CHAR(36)", nullable = false)
	private UUID appointmentId;

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
	@Column(name = "provider_id", columnDefinition = "CHAR(36)")
	private UUID providerId;
	@Column(name = "provider_name", length = 200)
	private String providerName;

	@JdbcTypeCode(SqlTypes.CHAR)
	@Column(name = "service_id", columnDefinition = "CHAR(36)")
	private UUID serviceId;
	@Column(name = "service_name", length = 120)
	private String serviceName;
	@Column(name = "visit_type_code", length = 40)
	private String visitTypeCode;

	@Column(name = "service_date", nullable = false)
	private LocalDate serviceDate;

	@Column(name = "unit_price", nullable = false, precision = 12, scale = 2)
	private BigDecimal unitPrice = BigDecimal.ZERO.setScale(2);
	@Column(name = "tax_rate_percent", nullable = false, precision = 5, scale = 2)
	private BigDecimal taxRatePercent = BigDecimal.ZERO.setScale(2);

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private ChargeStatus status = ChargeStatus.PENDING;

	@JdbcTypeCode(SqlTypes.CHAR)
	@Column(name = "package_id", columnDefinition = "CHAR(36)")
	private UUID packageId;

	@JdbcTypeCode(SqlTypes.CHAR)
	@Column(name = "invoice_id", columnDefinition = "CHAR(36)")
	private UUID invoiceId;

	@Column(length = 255)
	private String note;
}