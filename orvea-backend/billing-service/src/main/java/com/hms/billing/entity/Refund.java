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
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@SuppressWarnings("serial")
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "refunds", indexes = @Index(name = "idx_refund_invoice", columnList = "invoice_id"))
@SQLRestriction("is_deleted = false")
public class Refund extends BaseEntity {

	@JdbcTypeCode(SqlTypes.CHAR)
	@Column(name = "payment_id", columnDefinition = "CHAR(36)", nullable = false)
	private UUID paymentId;
	@JdbcTypeCode(SqlTypes.CHAR)
	@Column(name = "invoice_id", columnDefinition = "CHAR(36)", nullable = false)
	private UUID invoiceId;

	@Column(nullable = false, precision = 12, scale = 2)
	private BigDecimal amount;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 15)
	private PaymentMode mode;
	@Column(length = 100)
	private String reference;

	@Column(nullable = false, length = 255)
	private String reason;
	@Column(name = "refunded_on", nullable = false)
	private LocalDate refundedOn;
	@Column(name = "approved_by", nullable = false, length = 100)
	private String approvedBy;
}