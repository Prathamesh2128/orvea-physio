package com.hms.billing.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.hms.billing.dto.PaymentDtos.PaymentRequest;
import com.hms.billing.dto.PaymentDtos.PaymentView;
import com.hms.billing.dto.PaymentDtos.ReceiptResponse;
import com.hms.billing.dto.PaymentDtos.RefundRequest;
import com.hms.billing.dto.PaymentDtos.RefundView;
import com.hms.billing.service.PaymentService;
import com.hms.common.api.ApiResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/billing")
@RequiredArgsConstructor
public class PaymentController {

	private final PaymentService service;

	@PostMapping("/invoices/{invoiceId}/payments")
	@PreAuthorize("hasAuthority('PAYMENT_COLLECT')")
	public ResponseEntity<ApiResponse<PaymentView>> collect(@PathVariable UUID invoiceId,
			@Valid @RequestBody PaymentRequest req) {
		return ResponseEntity.status(HttpStatus.CREATED)
				.body(ApiResponse.created("Payment recorded", service.collect(invoiceId, req)));
	}

	@GetMapping("/invoices/{invoiceId}/payments")
	@PreAuthorize("hasAuthority('BILLING_READ')")
	public ApiResponse<List<PaymentView>> payments(@PathVariable UUID invoiceId) {
		return ApiResponse.ok(service.forInvoice(invoiceId));
	}

	@GetMapping("/payments/{paymentId}/receipt")
	@PreAuthorize("hasAuthority('BILLING_READ')")
	public ApiResponse<ReceiptResponse> receipt(@PathVariable UUID paymentId) {
		return ApiResponse.ok(service.receipt(paymentId));
	}

	@PostMapping("/payments/{paymentId}/refund")
	@PreAuthorize("hasAuthority('REFUND_APPROVE')")
	public ResponseEntity<ApiResponse<RefundView>> refund(@PathVariable UUID paymentId,
			@Valid @RequestBody RefundRequest req) {
		return ResponseEntity.status(HttpStatus.CREATED)
				.body(ApiResponse.created("Refund recorded", service.refund(paymentId, req)));
	}
}