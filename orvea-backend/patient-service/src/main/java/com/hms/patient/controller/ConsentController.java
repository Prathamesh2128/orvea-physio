package com.hms.patient.controller;

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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.hms.common.api.ApiResponse;
import com.hms.patient.dto.ConsentRequest;
import com.hms.patient.dto.ConsentResponse;
import com.hms.patient.dto.WithdrawConsentRequest;
import com.hms.patient.enums.ConsentType;
import com.hms.patient.service.ConsentService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/patients/{patientId}/consents")
@RequiredArgsConstructor
public class ConsentController {
	private final ConsentService service;

	@PostMapping
	@PreAuthorize("hasAuthority('PATIENT_UPDATE')")
	public ResponseEntity<ApiResponse<ConsentResponse>> grant(@PathVariable UUID patientId,
			@Valid @RequestBody ConsentRequest req) {
		return ResponseEntity.status(HttpStatus.CREATED)
				.body(ApiResponse.created("Consent recorded", service.grant(patientId, req)));
	}

	@GetMapping
	@PreAuthorize("hasAuthority('PATIENT_READ')")
	public ApiResponse<List<ConsentResponse>> list(@PathVariable UUID patientId) {
		return ApiResponse.ok(service.list(patientId));
	}

	@GetMapping("/check")
	@PreAuthorize("hasAuthority('PATIENT_READ')")
	public ApiResponse<Boolean> check(@PathVariable UUID patientId, @RequestParam ConsentType type) {
		return ApiResponse.ok(service.hasActiveConsent(patientId, type));
	}

	@PostMapping("/{consentId}/withdraw")
	@PreAuthorize("hasAuthority('PATIENT_UPDATE')")
	public ApiResponse<ConsentResponse> withdraw(@PathVariable UUID patientId, @PathVariable UUID consentId,
			@Valid @RequestBody WithdrawConsentRequest req) {
		return ApiResponse.ok("Consent withdrawn", service.withdraw(patientId, consentId, req));
	}
}
