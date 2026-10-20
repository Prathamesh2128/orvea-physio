package com.hms.patient.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.hms.common.api.ApiResponse;
import com.hms.patient.dto.PatientRequest;
import com.hms.patient.dto.PatientResponse;
import com.hms.patient.service.PatientService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/patients")
@RequiredArgsConstructor
public class PatientController {

	private final PatientService patientService;

	public ResponseEntity<ApiResponse<PatientResponse>> register(@Valid @RequestBody PatientRequest req) {
		PatientResponse data = patientService.register(req);
		return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created("Patient Created", data));
	}
}
