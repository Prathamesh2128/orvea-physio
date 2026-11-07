package com.hms.appointment.controller;

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

import com.hms.appointment.dto.VisitTypeRequest;
import com.hms.appointment.dto.VisitTypeResponse;
import com.hms.appointment.service.VisitTypeService;
import com.hms.common.api.ApiResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/appointments/visit-types")
@RequiredArgsConstructor
public class VisitTypeController {

	private final VisitTypeService service;

	@GetMapping
	@PreAuthorize("hasAnyAuthority('APPOINTMENT_READ','APPOINTMENT_CREATE','SETTINGS_MANAGE')")
	public ApiResponse<List<VisitTypeResponse>> list(@RequestParam(defaultValue = "true") boolean activeOnly) {
		return ApiResponse.ok(service.list(activeOnly));
	}

	@PostMapping
	@PreAuthorize("hasAuthority('SETTINGS_MANAGE')")
	public ResponseEntity<ApiResponse<VisitTypeResponse>> create(@Valid @RequestBody VisitTypeRequest req) {
		return ResponseEntity.status(HttpStatus.CREATED)
				.body(ApiResponse.created("Visit type created", service.create(req)));
	}

	@PutMapping("/{id}")
	@PreAuthorize("hasAuthority('SETTINGS_MANAGE')")
	public ApiResponse<VisitTypeResponse> update(@PathVariable UUID id, @Valid @RequestBody VisitTypeRequest req) {
		return ApiResponse.ok("Visit type updated", service.update(id, req));
	}

	@DeleteMapping("/{id}")
	@PreAuthorize("hasAuthority('SETTINGS_MANAGE')")
	public ApiResponse<Void> delete(@PathVariable UUID id) {
		service.delete(id);
		return ApiResponse.ok("Visit type deleted", null);
	}
}
