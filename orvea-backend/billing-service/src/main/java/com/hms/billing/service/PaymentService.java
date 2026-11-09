package com.hms.billing.service;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hms.billing.dto.PaymentDtos.PaymentRequest;
import com.hms.billing.dto.PaymentDtos.PaymentView;
import com.hms.billing.dto.PaymentDtos.ReceiptResponse;
import com.hms.billing.dto.PaymentDtos.RefundRequest;
import com.hms.billing.dto.PaymentDtos.RefundView;
import com.hms.billing.engine.InvoiceCalculator;
import com.hms.billing.engine.InvoiceStatusRules;
import com.hms.billing.engine.PaymentReference;
import com.hms.billing.entity.BillingSettings;
import com.hms.billing.entity.Invoice;
import com.hms.billing.entity.Payment;
import com.hms.billing.entity.Refund;
import com.hms.billing.enums.InvoiceStatus;
import com.hms.billing.enums.PaymentMode;
import com.hms.billing.repository.InvoiceRepository;
import com.hms.billing.repository.PaymentRepository;
import com.hms.billing.repository.RefundRepository;
import com.hms.common.audit.AuditPublisher;
import com.hms.common.exception.BusinessException;
import com.hms.common.exception.ResourceNotFoundException;
import com.hms.common.security.ClinicScope;
import com.hms.common.security.SecurityUtils;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PaymentService {

	private final InvoiceRepository invoices;
	private final PaymentRepository payments;
	private final RefundRepository refunds;
	private final NumberingService numbering;
	private final BillingSettingsService settingsService;
	private final PaymentQueries paymentQueries;
	private final AuditPublisher audit;
	private final Clock clock;

	// ===================================================== collect
	@Transactional
	public PaymentView collect(UUID invoiceId, PaymentRequest r) {
		Invoice inv = lockInvoice(invoiceId);
		if (inv.getStatus() != InvoiceStatus.ISSUED && inv.getStatus() != InvoiceStatus.PARTIAL) {
			throw new BusinessException(
					"Payments can only be taken on an issued invoice that still has a balance (this one is "
							+ inv.getStatus() + ")");
		}
		BigDecimal amount = positive(r.amount());
		BigDecimal balance = inv.getGrandTotal().subtract(inv.getPaidAmount());
		if (amount.compareTo(balance) > 0) {
			throw new BusinessException("The payment of " + amount.toPlainString() + " is more than the balance of "
					+ balance.toPlainString());
		}
		String reference = checkReference(r.mode(), r.reference());
		LocalDate today = LocalDate.now(clock);
		LocalDate on = r.receivedOn() == null ? today : r.receivedOn();
		if (on.isAfter(today))
			throw new BusinessException("The payment date cannot be in the future");
		if (on.isBefore(inv.getInvoiceDate()))
			throw new BusinessException("The payment date cannot be before the invoice date");

		BillingSettings s = settingsService.effective(inv.getClinicId());
		Payment p = new Payment();
		p.setClinicId(inv.getClinicId());
		p.setInvoiceId(inv.getId());
		p.setPatientId(inv.getPatientId());
		p.setReceiptNumber(numbering.next(s.getReceiptPrefix(), inv.getClinicId(), on, s.getFyStartMonth()));
		p.setAmount(amount);
		p.setMode(r.mode());
		p.setReference(reference);
		p.setReceivedOn(on);
		p.setReceivedBy(SecurityUtils.currentUsername());
		p.setNote(r.note());
		Payment saved = payments.save(p);

		inv.setPaidAmount(inv.getPaidAmount().add(amount));
		inv.setStatus(InvoiceStatusRules.forAmounts(inv.getGrandTotal(), inv.getPaidAmount()));
		invoices.save(inv);
		audit.record("PAYMENT_RECEIVED", "Payment", saved.getId(), Map.of("receipt", saved.getReceiptNumber(), "amount",
				amount.toPlainString(), "mode", r.mode().name(), "invoiceId", invoiceId.toString()));
		return view(saved);
	}

	// ===================================================== refund (admin)
	@Transactional
	public RefundView refund(UUID paymentId, RefundRequest r) {
		Payment p = payments.findById(paymentId).orElseThrow(() -> new ResourceNotFoundException("Payment", paymentId));
		ClinicScope.assertSameClinic(p.getClinicId(), "Payment", paymentId);
		Invoice inv = lockInvoice(p.getInvoiceId());
		BigDecimal amount = positive(r.amount());
		BigDecimal refundable = p.getAmount().subtract(p.getRefundedAmount());
		if (amount.compareTo(refundable) > 0) {
			throw new BusinessException(
					"Only " + refundable.toPlainString() + " of this payment can still be refunded");
		}
		if (r.reason() == null || r.reason().isBlank())
			throw new BusinessException("A reason is required for a refund");
		PaymentMode mode = r.mode() == null ? p.getMode() : r.mode();
		String reference = checkReference(mode, r.reference());

		Refund f = new Refund();
		f.setClinicId(p.getClinicId());
		f.setPaymentId(p.getId());
		f.setInvoiceId(inv.getId());
		f.setAmount(amount);
		f.setMode(mode);
		f.setReference(reference);
		f.setReason(r.reason().trim());
		f.setRefundedOn(LocalDate.now(clock));
		f.setApprovedBy(SecurityUtils.currentUsername());
		Refund saved = refunds.save(f);

		p.setRefundedAmount(p.getRefundedAmount().add(amount));
		payments.save(p);
		inv.setPaidAmount(inv.getPaidAmount().subtract(amount));
		if (inv.getStatus() != InvoiceStatus.CANCELLED)
			inv.setStatus(InvoiceStatusRules.forAmounts(inv.getGrandTotal(), inv.getPaidAmount()));
		invoices.save(inv);
		audit.record("REFUND_ISSUED", "Refund", saved.getId(), Map.of("paymentId", paymentId.toString(), "amount",
				amount.toPlainString(), "reason", r.reason().trim()));
		return new RefundView(saved.getId(), saved.getPaymentId(), saved.getAmount(), saved.getMode(),
				saved.getReference(), saved.getReason(), saved.getRefundedOn(), saved.getApprovedBy());
	}

	// ===================================================== read
	@Transactional(readOnly = true)
	public List<PaymentView> forInvoice(UUID invoiceId) {
		Invoice inv = invoices.findById(invoiceId)
				.orElseThrow(() -> new ResourceNotFoundException("Invoice", invoiceId));
		ClinicScope.assertSameClinic(inv.getClinicId(), "Invoice", invoiceId);
		return paymentQueries.payments(invoiceId);
	}

	@Transactional(readOnly = true)
	public ReceiptResponse receipt(UUID paymentId) {
		Payment p = payments.findById(paymentId).orElseThrow(() -> new ResourceNotFoundException("Payment", paymentId));
		ClinicScope.assertSameClinic(p.getClinicId(), "Payment", paymentId);
		Invoice inv = invoices.findById(p.getInvoiceId())
				.orElseThrow(() -> new ResourceNotFoundException("Invoice", p.getInvoiceId()));
		BillingSettings s = settingsService.effective(inv.getClinicId());
		return new ReceiptResponse(p.getReceiptNumber(), p.getReceivedOn(), inv.getPatientName(), inv.getPatientUhid(),
				inv.getInvoiceNumber(), p.getAmount(), p.getMode(), p.getReference(), p.getReceivedBy(),
				inv.getGrandTotal(), inv.getPaidAmount(), inv.getGrandTotal().subtract(inv.getPaidAmount()),
				settingsService.header(s));
	}

	// ===================================================== helpers
	private Invoice lockInvoice(UUID id) {
		Invoice inv = invoices.lockById(id).orElseThrow(() -> new ResourceNotFoundException("Invoice", id));
		ClinicScope.assertSameClinic(inv.getClinicId(), "Invoice", id);
		return inv;
	}

	private static BigDecimal positive(BigDecimal v) {
		BigDecimal a = v == null ? null : InvoiceCalculator.round2(v);
		if (a == null || a.signum() <= 0)
			throw new BusinessException("The amount must be more than zero");
		return a;
	}

	/**
	 * Cheques need their number; anything that looks like a card number is refused.
	 */
	private static String checkReference(PaymentMode mode, String reference) {
		String ref = reference == null || reference.isBlank() ? null : reference.trim();
		if (mode == PaymentMode.CHEQUE && ref == null)
			throw new BusinessException("Enter the cheque number");
		if (PaymentReference.looksLikeCardNumber(ref)) {
			throw new BusinessException(
					"That looks like a card number. Never enter card numbers: use the transaction id or the last 4 digits only.");
		}
		return ref;
	}

	private PaymentView view(Payment p) {
		return new PaymentView(p.getId(), p.getReceiptNumber(), p.getAmount(), p.getMode(), p.getReference(),
				p.getReceivedOn(), p.getReceivedBy(), p.getRefundedAmount());
	}
}