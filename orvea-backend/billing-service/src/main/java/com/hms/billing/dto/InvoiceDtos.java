package com.hms.billing.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import com.hms.billing.enums.InvoiceStatus;
import com.hms.billing.enums.LineType;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public final class InvoiceDtos {
	private InvoiceDtos() {
	}

	/**
	 * SERVICE: serviceId. PACKAGE: packageTemplateId. PRODUCT / OTHER: description
	 * + unitPrice (+ taxRatePercent). unitPrice may be left out to use the
	 * catalogue price; a different price needs a manager.
	 */
	public record LineRequest(LineType type, UUID serviceId, UUID packageTemplateId,
			@Size(max = 255) String description, Integer quantity, BigDecimal unitPrice, BigDecimal discountAmount,
			BigDecimal taxRatePercent) {
	}

	/**
	 * chargeIds = delivered visits from the pending list. issue=true issues the
	 * invoice straight away.
	 */
	public record CreateInvoiceRequest(@NotNull UUID patientId, UUID caseId, LocalDate dueDate,
			@Valid List<LineRequest> lines, List<UUID> chargeIds, @Size(max = 500) String notes, Boolean issue) {
	}

	public record InvoiceLineView(int lineNo, LineType type, String itemName, String description, int quantity,
			BigDecimal unitPrice, BigDecimal discountAmount, BigDecimal taxRatePercent, BigDecimal taxableAmount,
			BigDecimal taxAmount, BigDecimal lineTotal, UUID serviceId, UUID packageTemplateId, UUID chargeId) {
	}

	public record ClinicHeader(String name, String address, String phone, String email, String gstin,
			String registrationNo, String upiId, String footerNote) {
	}

	public record InvoiceResponse(UUID id, String invoiceNumber, InvoiceStatus status, LocalDate invoiceDate,
			LocalDate dueDate, UUID patientId, String patientName, String patientUhid, String patientPhone, UUID caseId,
			List<InvoiceLineView> lines, BigDecimal subtotal, BigDecimal discountTotal, BigDecimal taxableTotal,
			BigDecimal taxTotal, BigDecimal roundOff, BigDecimal grandTotal, BigDecimal paidAmount,
			BigDecimal balanceDue, List<PaymentDtos.PaymentView> payments, List<PaymentDtos.RefundView> refunds,
			String notes, ClinicHeader clinic, LocalDateTime issuedAt, String issuedBy, String cancelReason,
			LocalDateTime createdAt) {
	}

	public record InvoiceSummary(UUID id, String invoiceNumber, LocalDate invoiceDate, String patientName,
			String patientUhid, BigDecimal grandTotal, BigDecimal paidAmount, BigDecimal balanceDue,
			InvoiceStatus status) {
	}
}