package com.hms.patient.controller;

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

import com.hms.common.api.ApiResponse;
import com.hms.patient.dto.ConditionMasterRequest;
import com.hms.patient.dto.ConditionMasterResponse;
import com.hms.patient.service.ConditionMasterService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/conditions")
@RequiredArgsConstructor
public class ConditionMasterController {

	private final ConditionMasterService service;

	@GetMapping
	@PreAuthorize("hasAnyAuthority('PATIENT_READ','TREATMENT_READ','SETTINGS_MANAGE')")
	public ApiResponse<List<ConditionMasterResponse>> list(@RequestParam(defaultValue = "true") boolean activeOnly) {
		return ApiResponse.ok(service.list(activeOnly));
	}

	@PostMapping
	@PreAuthorize("hasAuthority('SETTINGS_MANAGE')")
	public ResponseEntity<ApiResponse<ConditionMasterResponse>> create(@Valid @RequestBody ConditionMasterRequest req) {
		return ResponseEntity.status(HttpStatus.CREATED)
				.body(ApiResponse.created("Condition created", service.create(req)));
	}

	@PutMapping("/{id}")
	@PreAuthorize("hasAuthority('SETTINGS_MANAGE')")
	public ApiResponse<ConditionMasterResponse> update(@PathVariable UUID id,
			@Valid @RequestBody ConditionMasterRequest req) {
		return ApiResponse.ok("Condition updated", service.update(id, req));
	}

	@DeleteMapping("/{id}")
	@PreAuthorize("hasAuthority('SETTINGS_MANAGE')")
	public ApiResponse<Void> delete(@PathVariable UUID id) {
		service.delete(id);
		return ApiResponse.ok("Condition deleted", null);
	}
}
