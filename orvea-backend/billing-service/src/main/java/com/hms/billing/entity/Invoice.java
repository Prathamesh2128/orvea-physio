package com.hms.billing.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.type.SqlTypes;

import com.hms.billing.enums.InvoiceStatus;
import com.hms.common.entity.BaseEntity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
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
@Table(name = "invoices", uniqueConstraints = @UniqueConstraint(name = "uk_invoice_clinic_number", columnNames = {
		"clinic_id", "invoice_number" }), indexes = { @Index(name = "idx_invoice_patient", columnList = "patient_id"),
				@Index(name = "idx_invoice_status_date", columnList = "status,invoice_date") })
@SQLRestriction("is_deleted = false")
public class Invoice extends BaseEntity {

	/**
	 * Assigned when the invoice is issued, so numbers have no gaps. Null for
	 * drafts.
	 */
	@Column(name = "invoice_number", length = 40)
	private String invoiceNumber;

	@JdbcTypeCode(SqlTypes.CHAR)
	@Column(name = "patient_id", columnDefinition = "CHAR(36)", nullable = false)
	private UUID patientId;
	@Column(name = "patient_name", length = 150)
	private String patientName;
	@Column(name = "patient_uhid", length = 30)
	private String patientUhid;
	@Column(name = "patient_phone", length = 20)
	private String patientPhone;

	@JdbcTypeCode(SqlTypes.CHAR)
	@Column(name = "case_id", columnDefinition = "CHAR(36)")
	private UUID caseId;

	@Column(name = "invoice_date", nullable = false)
	private LocalDate invoiceDate;
	@Column(name = "due_date")
	private LocalDate dueDate;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 12)
	private InvoiceStatus status = InvoiceStatus.DRAFT;

	@Column(nullable = false, precision = 12, scale = 2)
	private BigDecimal subtotal = BigDecimal.ZERO.setScale(2);
	@Column(name = "discount_total", nullable = false, precision = 12, scale = 2)
	private BigDecimal discountTotal = BigDecimal.ZERO.setScale(2);
	@Column(name = "taxable_total", nullable = false, precision = 12, scale = 2)
	private BigDecimal taxableTotal = BigDecimal.ZERO.setScale(2);
	@Column(name = "tax_total", nullable = false, precision = 12, scale = 2)
	private BigDecimal taxTotal = BigDecimal.ZERO.setScale(2);
	@Column(name = "round_off", nullable = false, precision = 12, scale = 2)
	private BigDecimal roundOff = BigDecimal.ZERO.setScale(2);
	@Column(name = "grand_total", nullable = false, precision = 12, scale = 2)
	private BigDecimal grandTotal = BigDecimal.ZERO.setScale(2);
	/** Net money received: payments minus refunds. */
	@Column(name = "paid_amount", nullable = false, precision = 12, scale = 2)
	private BigDecimal paidAmount = BigDecimal.ZERO.setScale(2);

	@Column(length = 500)
	private String notes;

	@Column(name = "issued_at")
	private LocalDateTime issuedAt;
	@Column(name = "issued_by", length = 100)
	private String issuedBy;

	@Column(name = "cancel_reason", length = 255)
	private String cancelReason;
	@Column(name = "cancelled_at")
	private LocalDateTime cancelledAt;

	@OneToMany(mappedBy = "invoice", cascade = CascadeType.ALL)
	@OrderBy("lineNo ASC")
	private List<InvoiceLine> lines = new ArrayList<>();
}