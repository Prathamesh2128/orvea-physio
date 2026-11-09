package com.hms.assessment.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.hms.assessment.dto.TemplateRequest;
import com.hms.assessment.dto.TemplateResponse;
import com.hms.assessment.dto.ValidateTemplateRequest;
import com.hms.assessment.dto.ValidateTemplateResponse;
import com.hms.assessment.service.TemplateService;
import com.hms.common.api.ApiResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/assessments/templates")
@RequiredArgsConstructor
public class TemplateController {

	private final TemplateService service;

	@GetMapping
	@PreAuthorize("hasAnyAuthority('ASSESSMENT_READ','ASSESSMENT_TEMPLATE_MANAGE')")
	public ApiResponse<List<TemplateResponse>> list() {
		return ApiResponse.ok(service.list());
	}

	@GetMapping("/{id}")
	@PreAuthorize("hasAnyAuthority('ASSESSMENT_READ','ASSESSMENT_TEMPLATE_MANAGE')")
	public ApiResponse<TemplateResponse> get(@PathVariable UUID id) {
		return ApiResponse.ok(service.get(id));
	}

	@GetMapping("/code/{code}/versions")
	@PreAuthorize("hasAuthority('ASSESSMENT_TEMPLATE_MANAGE')")
	public ApiResponse<List<TemplateResponse>> versions(@PathVariable String code) {
		return ApiResponse.ok(service.versions(code));
	}

	@PostMapping
	@PreAuthorize("hasAuthority('ASSESSMENT_TEMPLATE_MANAGE')")
	public ResponseEntity<ApiResponse<TemplateResponse>> create(@Valid @RequestBody TemplateRequest req) {
		return ResponseEntity.status(HttpStatus.CREATED)
				.body(ApiResponse.created("Template created as a draft", service.create(req)));
	}

	@PutMapping("/{id}")
	@PreAuthorize("hasAuthority('ASSESSMENT_TEMPLATE_MANAGE')")
	public ApiResponse<TemplateResponse> update(@PathVariable UUID id, @Valid @RequestBody TemplateRequest req) {
		return ApiResponse.ok("Template saved", service.update(id, req));
	}

	@PostMapping("/{id}/publish")
	@PreAuthorize("hasAuthority('ASSESSMENT_TEMPLATE_MANAGE')")
	public ApiResponse<TemplateResponse> publish(@PathVariable UUID id) {
		return ApiResponse.ok("Template published", service.publish(id));
	}

	@PostMapping("/{id}/archive")
	@PreAuthorize("hasAuthority('ASSESSMENT_TEMPLATE_MANAGE')")
	public ApiResponse<TemplateResponse> archive(@PathVariable UUID id) {
		return ApiResponse.ok("Template archived", service.archive(id));
	}

	@PostMapping("/{id}/customize")
	@PreAuthorize("hasAuthority('ASSESSMENT_TEMPLATE_MANAGE')")
	public ResponseEntity<ApiResponse<TemplateResponse>> customize(@PathVariable UUID id) {
		return ResponseEntity.status(HttpStatus.CREATED)
				.body(ApiResponse.created("Copied for your clinic as a draft", service.customize(id)));
	}

	@PostMapping("/validate")
	@PreAuthorize("hasAuthority('ASSESSMENT_TEMPLATE_MANAGE')")
	public ApiResponse<ValidateTemplateResponse> validate(@Valid @RequestBody ValidateTemplateRequest req) {
		return ApiResponse.ok(service.validate(req.definition()));
	}
}