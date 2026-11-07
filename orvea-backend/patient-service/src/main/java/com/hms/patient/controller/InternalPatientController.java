package com.hms.patient.controller;

import java.util.UUID;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.hms.common.api.ApiResponse;
import com.hms.patient.dto.PatientRef;
import com.hms.patient.service.PatientService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/internal/patients")
@RequiredArgsConstructor
public class InternalPatientController {

	private final PatientService patientService;

	@GetMapping("/{id}")
	@PreAuthorize("isAuthenticated()")
	public ApiResponse<PatientRef> ref(@PathVariable UUID id) {
		return ApiResponse.ok(patientService.getRef(id));
	}
}
