package com.hms.billing.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.hms.billing.dto.BillingDtos.PackageView;
import com.hms.billing.dto.BillingDtos.ReasonRequest;
import com.hms.billing.dto.BillingDtos.UsageView;
import com.hms.billing.service.PackageService;
import com.hms.common.api.ApiResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/billing/packages")
@RequiredArgsConstructor
public class PackageController {

	private final PackageService service;

	@GetMapping("/patient/{patientId}")
	@PreAuthorize("hasAuthority('BILLING_READ')")
	public ApiResponse<List<PackageView>> forPatient(@PathVariable UUID patientId) {
		return ApiResponse.ok(service.forPatient(patientId));
	}

	@GetMapping("/{id}/usages")
	@PreAuthorize("hasAuthority('BILLING_READ')")
	public ApiResponse<List<UsageView>> usages(@PathVariable UUID id) {
		return ApiResponse.ok(service.usagesOf(id));
	}

	@PostMapping("/{id}/usages/{usageId}/reverse")
	@PreAuthorize("hasAuthority('BILLING_UPDATE')")
	public ApiResponse<PackageView> reverse(@PathVariable UUID id, @PathVariable UUID usageId,
			@Valid @RequestBody ReasonRequest req) {
		return ApiResponse.ok("Session reversed", service.reverseUsage(id, usageId, req.reason()));
	}

	@PostMapping("/{id}/cancel")
	@PreAuthorize("hasAuthority('BILLING_UPDATE')")
	public ApiResponse<PackageView> cancel(@PathVariable UUID id, @Valid @RequestBody ReasonRequest req) {
		return ApiResponse.ok("Package cancelled", service.cancel(id, req.reason()));
	}
}