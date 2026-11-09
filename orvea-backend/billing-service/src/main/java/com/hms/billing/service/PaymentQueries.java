package com.hms.billing.service;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.hms.billing.dto.PaymentDtos.PaymentView;
import com.hms.billing.dto.PaymentDtos.RefundView;
import com.hms.billing.repository.PaymentRepository;
import com.hms.billing.repository.RefundRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class PaymentQueries {

	private final PaymentRepository payments;
	private final RefundRepository refunds;

	public List<PaymentView> payments(UUID invoiceId) {
		return payments.findByInvoiceIdOrderByCreatedAtAsc(invoiceId).stream()
				.map(p -> new PaymentView(p.getId(), p.getReceiptNumber(), p.getAmount(), p.getMode(), p.getReference(),
						p.getReceivedOn(), p.getReceivedBy(), p.getRefundedAmount()))
				.toList();
	}

	public List<RefundView> refunds(UUID invoiceId) {
		return refunds.findByInvoiceIdOrderByCreatedAtAsc(invoiceId).stream()
				.map(r -> new RefundView(r.getId(), r.getPaymentId(), r.getAmount(), r.getMode(), r.getReference(),
						r.getReason(), r.getRefundedOn(), r.getApprovedBy()))
				.toList();
	}
}