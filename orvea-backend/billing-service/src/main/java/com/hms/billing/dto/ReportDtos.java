package com.hms.billing.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import com.hms.billing.enums.LineType;
import com.hms.billing.enums.PaymentMode;

public final class ReportDtos {
	private ReportDtos() {
	}

	public record CollectionRow(PaymentMode mode, BigDecimal received, BigDecimal refunded, BigDecimal net,
			long payments) {
	}

	public record CollectionReport(LocalDate from, LocalDate to, List<CollectionRow> rows, BigDecimal totalReceived,
			BigDecimal totalRefunded, BigDecimal totalNet) {
	}

	public record RevenueRow(LineType type, String item, long quantity, BigDecimal taxable, BigDecimal tax,
			BigDecimal total) {
	}

	public record RevenueReport(LocalDate from, LocalDate to, List<RevenueRow> rows, BigDecimal totalTaxable,
			BigDecimal totalTax, BigDecimal grandTotal) {
	}

	/** Ageing columns: 0-30, 31-60, 61-90, over 90 days since the invoice date. */
	public record DueRow(UUID patientId, String patientName, String patientUhid, String patientPhone, int invoices,
			LocalDate oldestInvoiceDate, BigDecimal totalDue, BigDecimal age0to30, BigDecimal age31to60,
			BigDecimal age61to90, BigDecimal ageOver90) {
	}

	public record DueReport(LocalDate asOf, List<DueRow> rows, BigDecimal totalOutstanding, BigDecimal age0to30,
			BigDecimal age31to60, BigDecimal age61to90, BigDecimal ageOver90) {
	}
}