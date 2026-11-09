package com.hms.billing.service;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hms.billing.dto.ReportDtos.CollectionReport;
import com.hms.billing.dto.ReportDtos.CollectionRow;
import com.hms.billing.dto.ReportDtos.DueReport;
import com.hms.billing.dto.ReportDtos.DueRow;
import com.hms.billing.dto.ReportDtos.RevenueReport;
import com.hms.billing.dto.ReportDtos.RevenueRow;
import com.hms.billing.engine.Aging;
import com.hms.billing.entity.Invoice;
import com.hms.billing.enums.InvoiceStatus;
import com.hms.billing.enums.LineType;
import com.hms.billing.enums.PaymentMode;
import com.hms.billing.repository.InvoiceLineRepository;
import com.hms.billing.repository.InvoiceRepository;
import com.hms.billing.repository.PaymentRepository;
import com.hms.billing.repository.RefundRepository;
import com.hms.common.exception.BusinessException;
import com.hms.common.security.ClinicScope;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ReportService {

	private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2);
	private static final List<InvoiceStatus> COUNTED = List.of(InvoiceStatus.ISSUED, InvoiceStatus.PARTIAL,
			InvoiceStatus.PAID);

	private final PaymentRepository payments;
	private final RefundRepository refunds;
	private final InvoiceLineRepository lines;
	private final InvoiceRepository invoices;
	private final Clock clock;

	/** End-of-day cash book: money received and refunded per payment mode. */
	@Transactional(readOnly = true)
	public CollectionReport collection(LocalDate from, LocalDate to) {
		checkRange(from, to);
		UUID clinic = ClinicScope.currentClinicId();
		Map<PaymentMode, BigDecimal> received = new EnumMap<>(PaymentMode.class);
		Map<PaymentMode, Long> counts = new EnumMap<>(PaymentMode.class);
		for (Object[] row : payments.receivedByMode(from, to, clinic)) {
			received.put((PaymentMode) row[0], (BigDecimal) row[1]);
			counts.put((PaymentMode) row[0], (Long) row[2]);
		}
		Map<PaymentMode, BigDecimal> refunded = new EnumMap<>(PaymentMode.class);
		for (Object[] row : refunds.refundedByMode(from, to, clinic))
			refunded.put((PaymentMode) row[0], (BigDecimal) row[1]);

		List<CollectionRow> rows = new ArrayList<>();
		BigDecimal tr = ZERO, tf = ZERO;
		for (PaymentMode m : PaymentMode.values()) {
			BigDecimal rec = received.getOrDefault(m, ZERO);
			BigDecimal ref = refunded.getOrDefault(m, ZERO);
			if (rec.signum() == 0 && ref.signum() == 0)
				continue;
			rows.add(new CollectionRow(m, rec, ref, rec.subtract(ref), counts.getOrDefault(m, 0L)));
			tr = tr.add(rec);
			tf = tf.add(ref);
		}
		return new CollectionReport(from, to, rows, tr, tf, tr.subtract(tf));
	}

	/** What was billed (not what was collected), by item, for issued invoices. */
	@Transactional(readOnly = true)
	public RevenueReport revenue(LocalDate from, LocalDate to) {
		checkRange(from, to);
		List<RevenueRow> rows = new ArrayList<>();
		BigDecimal taxable = ZERO, tax = ZERO, total = ZERO;
		for (Object[] r : lines.revenueByItem(COUNTED, from, to, ClinicScope.currentClinicId())) {
			RevenueRow row = new RevenueRow((LineType) r[0], (String) r[1], ((Number) r[2]).longValue(),
					(BigDecimal) r[3], (BigDecimal) r[4], (BigDecimal) r[5]);
			rows.add(row);
			taxable = taxable.add(row.taxable());
			tax = tax.add(row.tax());
			total = total.add(row.total());
		}
		return new RevenueReport(from, to, rows, taxable, tax, total);
	}

	/** Who still owes money, per patient, with ageing from the invoice date. */
	@Transactional(readOnly = true)
	public DueReport dues() {
		LocalDate today = LocalDate.now(clock);
		Map<UUID, List<Invoice>> byPatient = new LinkedHashMap<>();
		for (Invoice i : invoices.findByStatuses(List.of(InvoiceStatus.ISSUED, InvoiceStatus.PARTIAL),
				ClinicScope.currentClinicId())) {
			byPatient.computeIfAbsent(i.getPatientId(), k -> new ArrayList<>()).add(i);
		}
		List<DueRow> rows = new ArrayList<>();
		BigDecimal[] grand = { ZERO, ZERO, ZERO, ZERO };
		for (List<Invoice> list : byPatient.values()) {
			BigDecimal[] age = { ZERO, ZERO, ZERO, ZERO };
			LocalDate oldest = null;
			for (Invoice i : list) {
				BigDecimal balance = i.getGrandTotal().subtract(i.getPaidAmount());
				int b = Aging.bucket(ChronoUnit.DAYS.between(i.getInvoiceDate(), today));
				age[b] = age[b].add(balance);
				grand[b] = grand[b].add(balance);
				if (oldest == null || i.getInvoiceDate().isBefore(oldest))
					oldest = i.getInvoiceDate();
			}
			Invoice first = list.get(0);
			rows.add(new DueRow(first.getPatientId(), first.getPatientName(), first.getPatientUhid(),
					first.getPatientPhone(), list.size(), oldest, age[0].add(age[1]).add(age[2]).add(age[3]), age[0],
					age[1], age[2], age[3]));
		}
		rows.sort(Comparator.comparing(DueRow::totalDue).reversed());
		return new DueReport(today, rows, grand[0].add(grand[1]).add(grand[2]).add(grand[3]), grand[0], grand[1],
				grand[2], grand[3]);
	}

	private void checkRange(LocalDate from, LocalDate to) {
		if (from == null || to == null || to.isBefore(from))
			throw new BusinessException("Choose a valid date range");
		if (ChronoUnit.DAYS.between(from, to) > 366)
			throw new BusinessException("A report can cover at most one year");
	}
}