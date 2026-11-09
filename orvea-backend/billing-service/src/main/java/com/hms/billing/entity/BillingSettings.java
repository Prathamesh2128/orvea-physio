package com.hms.billing.entity;

import java.math.BigDecimal;

import org.hibernate.annotations.SQLRestriction;

import com.hms.common.entity.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** The clinic's letterhead and billing rules. Everything is editable. */
@SuppressWarnings("serial")
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "billing_settings")
@SQLRestriction("is_deleted = false")
public class BillingSettings extends BaseEntity {

	@Column(name = "clinic_name", length = 120)
	private String clinicName;
	@Column(length = 255)
	private String address;
	@Column(length = 30)
	private String phone;
	@Column(length = 100)
	private String email;
	@Column(length = 20)
	private String gstin;
	@Column(name = "registration_no", length = 60)
	private String registrationNo;
	@Column(name = "upi_id", length = 80)
	private String upiId;

	@Column(name = "invoice_prefix", nullable = false, length = 10)
	private String invoicePrefix = "INV";
	@Column(name = "receipt_prefix", nullable = false, length = 10)
	private String receiptPrefix = "RCP";
	/** 4 = April (Indian financial year). */
	@Column(name = "fy_start_month", nullable = false)
	private int fyStartMonth = 4;
	@Column(name = "round_off_to_rupee", nullable = false)
	private boolean roundOffToRupee = true;
	/** A discount above this share of the bill needs a manager. */
	@Column(name = "discount_approval_percent", nullable = false, precision = 5, scale = 2)
	private BigDecimal discountApprovalPercent = new BigDecimal("10.00");
	/** 0 = payable at once; otherwise the due date is invoice date + this. */
	@Column(name = "payment_due_days", nullable = false)
	private int paymentDueDays = 0;
	@Column(name = "footer_note", length = 300)
	private String footerNote;
}