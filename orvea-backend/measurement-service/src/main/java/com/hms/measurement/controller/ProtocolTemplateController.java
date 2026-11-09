package com.hms.measurement.controller;

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
import org.springframework.web.bind.annotation.RestController;

import com.hms.common.api.ApiResponse;
import com.hms.measurement.dto.TemplateDtos.TemplateRequest;
import com.hms.measurement.dto.TemplateDtos.TemplateResponse;
import com.hms.measurement.service.ProtocolTemplateService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/measurements/protocol-templates")
@RequiredArgsConstructor
public class ProtocolTemplateController {

	private final ProtocolTemplateService service;

	@GetMapping
	@PreAuthorize("hasAnyAuthority('MEASUREMENT_PLAN','MEASUREMENT_SETTINGS')")
	public ApiResponse<List<TemplateResponse>> list() {
		return ApiResponse.ok(service.list());
	}

	@PostMapping
	@PreAuthorize("hasAnyAuthority('MEASUREMENT_PLAN','MEASUREMENT_SETTINGS')")
	public ResponseEntity<ApiResponse<TemplateResponse>> create(@Valid @RequestBody TemplateRequest req) {
		return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created("Preset saved", service.create(req)));
	}

	@PutMapping("/{id}")
	@PreAuthorize("hasAnyAuthority('MEASUREMENT_PLAN','MEASUREMENT_SETTINGS')")
	public ApiResponse<TemplateResponse> update(@PathVariable UUID id, @Valid @RequestBody TemplateRequest req) {
		return ApiResponse.ok("Preset updated", service.update(id, req));
	}

	@DeleteMapping("/{id}")
	@PreAuthorize("hasAnyAuthority('MEASUREMENT_PLAN','MEASUREMENT_SETTINGS')")
	public ApiResponse<Void> delete(@PathVariable UUID id) {
		service.delete(id);
		return ApiResponse.ok("Preset removed", null);
	}
}