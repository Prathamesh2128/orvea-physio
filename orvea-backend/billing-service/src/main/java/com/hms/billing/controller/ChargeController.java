package com.hms.billing.controller;

import java.util.UUID;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.hms.billing.dto.BillingDtos.AssignServiceRequest;
import com.hms.billing.dto.BillingDtos.ChargeView;
import com.hms.billing.dto.BillingDtos.CoverRequest;
import com.hms.billing.dto.BillingDtos.ReasonRequest;
import com.hms.billing.enums.ChargeStatus;
import com.hms.billing.service.ChargeService;
import com.hms.common.api.ApiResponse;
import com.hms.common.api.PageResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/billing/charges")
@RequiredArgsConstructor
public class ChargeController {

	private final ChargeService service;

	/**
	 * The "pending charges" screen: status=PENDING (and UNMAPPED for visits that
	 * need a service).
	 */
	@GetMapping
	@PreAuthorize("hasAuthority('BILLING_READ')")
	public ApiResponse<PageResponse<ChargeView>> list(@RequestParam(required = false) UUID patientId,
			@RequestParam(required = false) ChargeStatus status, @RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "50") int size) {
		return ApiResponse.ok(service.list(patientId, status,
				PageRequest.of(page, Math.min(size, 200), Sort.by("serviceDate").descending())));
	}

	@PostMapping("/{id}/waive")
	@PreAuthorize("hasAuthority('BILLING_UPDATE')")
	public ApiResponse<ChargeView> waive(@PathVariable UUID id, @Valid @RequestBody ReasonRequest req) {
		return ApiResponse.ok("Charge waived", service.waive(id, req.reason()));
	}

	@PostMapping("/{id}/service")
	@PreAuthorize("hasAuthority('BILLING_UPDATE')")
	public ApiResponse<ChargeView> assignService(@PathVariable UUID id, @Valid @RequestBody AssignServiceRequest req) {
		return ApiResponse.ok("Service set", service.assignService(id, req.serviceId()));
	}

	@PostMapping("/{id}/use-package")
	@PreAuthorize("hasAuthority('BILLING_CREATE')")
	public ApiResponse<ChargeView> usePackage(@PathVariable UUID id, @Valid @RequestBody CoverRequest req) {
		return ApiResponse.ok("Package session used", service.coverWithPackage(id, req.packageId()));
	}
}