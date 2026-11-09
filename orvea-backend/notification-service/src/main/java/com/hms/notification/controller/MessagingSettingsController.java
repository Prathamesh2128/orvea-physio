package com.hms.notification.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.hms.common.api.ApiResponse;
import com.hms.notification.dto.MessagingSettingsRequest;
import com.hms.notification.dto.MessagingSettingsResponse;
import com.hms.notification.service.MessagingSettingsService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/notifications/settings")
@PreAuthorize("hasAuthority('SETTINGS_MANAGE')")
@RequiredArgsConstructor
public class MessagingSettingsController {

	private final MessagingSettingsService service;

	@GetMapping
	public ApiResponse<MessagingSettingsResponse> get() {
		return ApiResponse.ok(service.get());
	}

	@PutMapping
	public ApiResponse<MessagingSettingsResponse> update(@Valid @RequestBody MessagingSettingsRequest req) {
		return ApiResponse.ok("Messaging settings saved", service.update(req));
	}
}
