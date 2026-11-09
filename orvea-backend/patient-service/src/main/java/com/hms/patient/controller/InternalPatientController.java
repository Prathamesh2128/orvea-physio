package com.hms.patient.controller;

import java.util.UUID;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.hms.common.api.ApiResponse;
import com.hms.patient.dto.CaseRef;
import com.hms.patient.dto.PatientRef;
import com.hms.patient.enums.ConsentType;
import com.hms.patient.service.ConsentService;
import com.hms.patient.service.PatientService;
import com.hms.patient.service.TreatmentCaseService;

import lombok.RequiredArgsConstructor;

/** Service-to-service only. The gateway does not route /api/v1/internal/**. */
@RestController
@RequestMapping("/api/v1/internal/patients")
@RequiredArgsConstructor
public class InternalPatientController {

	private final PatientService patientService;
	private final ConsentService consentService;
	private final TreatmentCaseService caseService;

	@GetMapping("/{id}")
	@PreAuthorize("isAuthenticated()")
	public ApiResponse<PatientRef> ref(@PathVariable UUID id) {
		return ApiResponse.ok(patientService.getRef(id));
	}

	@GetMapping("/{id}/consent")
	@PreAuthorize("isAuthenticated()")
	public ApiResponse<Boolean> consent(@PathVariable UUID id, @RequestParam ConsentType type) {
		return ApiResponse.ok(consentService.hasActiveConsent(id, type));
	}

	@GetMapping("/{id}/cases/{caseId}")
	@PreAuthorize("isAuthenticated()")
	public ApiResponse<CaseRef> caseRef(@PathVariable UUID id, @PathVariable UUID caseId) {
		return ApiResponse.ok(caseService.getRef(id, caseId));
	}
}
