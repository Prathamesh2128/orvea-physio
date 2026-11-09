package com.hms.billing.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import com.hms.billing.enums.PaymentMode;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public final class PaymentDtos {
	private PaymentDtos() {
	}

	/**
	 * reference: UPI / bank transaction id, cheque number or last 4 digits. NEVER a
	 * card number.
	 */
	public record PaymentRequest(@NotNull BigDecimal amount, @NotNull PaymentMode mode,
			@Size(max = 100) String reference, LocalDate receivedOn, @Size(max = 255) String note) {
	}

	public record RefundRequest(@NotNull BigDecimal amount, PaymentMode mode, @Size(max = 100) String reference,
			@NotBlank @Size(max = 255) String reason) {
	}

	public record PaymentView(UUID id, String receiptNumber, BigDecimal amount, PaymentMode mode, String reference,
			LocalDate receivedOn, String receivedBy, BigDecimal refundedAmount) {
	}

	public record RefundView(UUID id, UUID paymentId, BigDecimal amount, PaymentMode mode, String reference,
			String reason, LocalDate refundedOn, String approvedBy) {
	}

	public record ReceiptResponse(String receiptNumber, LocalDate receivedOn, String patientName, String patientUhid,
			String invoiceNumber, BigDecimal amount, PaymentMode mode, String reference, String receivedBy,
			BigDecimal invoiceTotal, BigDecimal invoicePaidSoFar, BigDecimal invoiceBalance,
			InvoiceDtos.ClinicHeader clinic) {
	}
}