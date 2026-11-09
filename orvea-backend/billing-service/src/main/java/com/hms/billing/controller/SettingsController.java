package com.hms.billing.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.hms.billing.dto.BillingDtos.SettingsRequest;
import com.hms.billing.dto.BillingDtos.SettingsResponse;
import com.hms.billing.service.BillingSettingsService;
import com.hms.common.api.ApiResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/billing/settings")
@RequiredArgsConstructor
public class SettingsController {

	private final BillingSettingsService service;

	@GetMapping
	@PreAuthorize("hasAnyAuthority('SETTINGS_MANAGE','BILLING_READ')")
	public ApiResponse<SettingsResponse> get() {
		return ApiResponse.ok(service.get());
	}

	@PutMapping
	@PreAuthorize("hasAuthority('SETTINGS_MANAGE')")
	public ApiResponse<SettingsResponse> update(@Valid @RequestBody SettingsRequest req) {
		return ApiResponse.ok("Billing settings saved", service.update(req));
	}
}