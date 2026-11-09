package com.hms.billing.controller;

import java.util.List;
import java.util.UUID;

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

import com.hms.billing.dto.BillingDtos.PackageTemplateRequest;
import com.hms.billing.dto.BillingDtos.PackageTemplateView;
import com.hms.billing.dto.BillingDtos.ServiceRequest;
import com.hms.billing.dto.BillingDtos.ServiceView;
import com.hms.billing.service.CatalogService;
import com.hms.common.api.ApiResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/billing/catalog")
@RequiredArgsConstructor
public class CatalogController {

	private final CatalogService service;

	@GetMapping("/services")
	@PreAuthorize("hasAnyAuthority('BILLING_READ','SETTINGS_MANAGE')")
	public ApiResponse<List<ServiceView>> services(@RequestParam(defaultValue = "true") boolean activeOnly) {
		return ApiResponse.ok(service.services(activeOnly));
	}

	@PostMapping("/services")
	@PreAuthorize("hasAuthority('SETTINGS_MANAGE')")
	public ResponseEntity<ApiResponse<ServiceView>> createService(@Valid @RequestBody ServiceRequest req) {
		return ResponseEntity.status(HttpStatus.CREATED)
				.body(ApiResponse.created("Service added", service.createService(req)));
	}

	@PutMapping("/services/{id}")
	@PreAuthorize("hasAuthority('SETTINGS_MANAGE')")
	public ApiResponse<ServiceView> updateService(@PathVariable UUID id, @Valid @RequestBody ServiceRequest req) {
		return ApiResponse.ok("Service updated", service.updateService(id, req));
	}

	@DeleteMapping("/services/{id}")
	@PreAuthorize("hasAuthority('SETTINGS_MANAGE')")
	public ApiResponse<Void> deleteService(@PathVariable UUID id) {
		service.deactivateService(id);
		return ApiResponse.ok("Service removed", null);
	}

	@GetMapping("/packages")
	@PreAuthorize("hasAnyAuthority('BILLING_READ','SETTINGS_MANAGE')")
	public ApiResponse<List<PackageTemplateView>> packages(@RequestParam(defaultValue = "true") boolean activeOnly) {
		return ApiResponse.ok(service.packages(activeOnly));
	}

	@PostMapping("/packages")
	@PreAuthorize("hasAuthority('SETTINGS_MANAGE')")
	public ResponseEntity<ApiResponse<PackageTemplateView>> createPackage(
			@Valid @RequestBody PackageTemplateRequest req) {
		return ResponseEntity.status(HttpStatus.CREATED)
				.body(ApiResponse.created("Package added", service.createPackage(req)));
	}

	@PutMapping("/packages/{id}")
	@PreAuthorize("hasAuthority('SETTINGS_MANAGE')")
	public ApiResponse<PackageTemplateView> updatePackage(@PathVariable UUID id,
			@Valid @RequestBody PackageTemplateRequest req) {
		return ApiResponse.ok("Package updated", service.updatePackage(id, req));
	}

	@DeleteMapping("/packages/{id}")
	@PreAuthorize("hasAuthority('SETTINGS_MANAGE')")
	public ApiResponse<Void> deletePackage(@PathVariable UUID id) {
		service.deactivatePackage(id);
		return ApiResponse.ok("Package removed", null);
	}
}