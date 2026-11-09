package com.hms.billing.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.type.SqlTypes;

import com.hms.billing.enums.PaymentMode;
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

/** Money received. Never edited: a mistake is refunded. */
@SuppressWarnings("serial")
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "payments", uniqueConstraints = @UniqueConstraint(name = "uk_payment_clinic_receipt", columnNames = {
		"clinic_id", "receipt_number" }), indexes = { @Index(name = "idx_payment_invoice", columnList = "invoice_id"),
				@Index(name = "idx_payment_date", columnList = "received_on,mode") })
@SQLRestriction("is_deleted = false")
public class Payment extends BaseEntity {

	@Column(name = "receipt_number", nullable = false, length = 40)
	private String receiptNumber;

	@JdbcTypeCode(SqlTypes.CHAR)
	@Column(name = "invoice_id", columnDefinition = "CHAR(36)", nullable = false)
	private UUID invoiceId;
	@JdbcTypeCode(SqlTypes.CHAR)
	@Column(name = "patient_id", columnDefinition = "CHAR(36)", nullable = false)
	private UUID patientId;

	@Column(nullable = false, precision = 12, scale = 2)
	private BigDecimal amount;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 15)
	private PaymentMode mode;

	/**
	 * UPI / bank transaction id, cheque number or last 4 digits. Never a card
	 * number.
	 */
	@Column(length = 100)
	private String reference;

	@Column(name = "received_on", nullable = false)
	private LocalDate receivedOn;
	@Column(name = "received_by", nullable = false, length = 100)
	private String receivedBy;

	@Column(length = 255)
	private String note;

	@Column(name = "refunded_amount", nullable = false, precision = 12, scale = 2)
	private BigDecimal refundedAmount = BigDecimal.ZERO.setScale(2);
}