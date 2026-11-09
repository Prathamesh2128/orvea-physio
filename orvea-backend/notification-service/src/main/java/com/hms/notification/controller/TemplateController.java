package com.hms.notification.controller;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.hms.common.api.ApiResponse;
import com.hms.notification.dto.TemplateRequest;
import com.hms.notification.dto.TemplateResponse;
import com.hms.notification.enums.MessageType;
import com.hms.notification.service.TemplateService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/notifications/templates")
@PreAuthorize("hasAuthority('SETTINGS_MANAGE')")
@RequiredArgsConstructor
public class TemplateController {

	private final TemplateService service;

	@GetMapping
	public ApiResponse<List<TemplateResponse>> list() {
		return ApiResponse.ok(service.list());
	}

	@PutMapping("/{type}")
	public ApiResponse<TemplateResponse> update(@PathVariable MessageType type,
			@Valid @RequestBody TemplateRequest req) {
		return ApiResponse.ok("Template saved", service.update(type, req));
	}
}
