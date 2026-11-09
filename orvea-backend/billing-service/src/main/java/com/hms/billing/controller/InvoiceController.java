package com.hms.billing.controller;

import java.time.LocalDate;
import java.util.UUID;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.hms.billing.dto.BillingDtos.ReasonRequest;
import com.hms.billing.dto.InvoiceDtos.CreateInvoiceRequest;
import com.hms.billing.dto.InvoiceDtos.InvoiceResponse;
import com.hms.billing.dto.InvoiceDtos.InvoiceSummary;
import com.hms.billing.enums.InvoiceStatus;
import com.hms.billing.service.InvoiceService;
import com.hms.common.api.ApiResponse;
import com.hms.common.api.PageResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/billing/invoices")
@RequiredArgsConstructor
public class InvoiceController {

	private final InvoiceService service;

	@PostMapping
	@PreAuthorize("hasAuthority('BILLING_CREATE')")
	public ResponseEntity<ApiResponse<InvoiceResponse>> create(@Valid @RequestBody CreateInvoiceRequest req) {
		return ResponseEntity.status(HttpStatus.CREATED)
				.body(ApiResponse.created("Invoice saved", service.create(req)));
	}

	@GetMapping
	@PreAuthorize("hasAuthority('BILLING_READ')")
	public ApiResponse<PageResponse<InvoiceSummary>> search(@RequestParam(required = false) UUID patientId,
			@RequestParam(required = false) InvoiceStatus status,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
			@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
		return ApiResponse.ok(service.search(patientId, status, from, to, PageRequest.of(page, Math.min(size, 100),
				Sort.by("invoiceDate").descending().and(Sort.by("createdAt").descending()))));
	}

	@GetMapping("/{id}")
	@PreAuthorize("hasAuthority('BILLING_READ')")
	public ApiResponse<InvoiceResponse> get(@PathVariable UUID id) {
		return ApiResponse.ok(service.get(id));
	}

	@PutMapping("/{id}")
	@PreAuthorize("hasAuthority('BILLING_CREATE')")
	public ApiResponse<InvoiceResponse> update(@PathVariable UUID id, @Valid @RequestBody CreateInvoiceRequest req) {
		return ApiResponse.ok("Draft updated", service.update(id, req));
	}

	@PostMapping("/{id}/issue")
	@PreAuthorize("hasAuthority('BILLING_CREATE')")
	public ApiResponse<InvoiceResponse> issue(@PathVariable UUID id) {
		return ApiResponse.ok("Invoice issued", service.issue(id));
	}

	@PostMapping("/{id}/cancel")
	@PreAuthorize("hasAuthority('BILLING_UPDATE')")
	public ApiResponse<InvoiceResponse> cancel(@PathVariable UUID id, @Valid @RequestBody ReasonRequest req) {
		return ApiResponse.ok("Invoice cancelled", service.cancel(id, req.reason()));
	}

	@DeleteMapping("/{id}")
	@PreAuthorize("hasAuthority('BILLING_CREATE')")
	public ApiResponse<Void> deleteDraft(@PathVariable UUID id) {
		service.deleteDraft(id);
		return ApiResponse.ok("Draft deleted", null);
	}
}