package com.hms.appointment.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.hms.appointment.dto.SchedulingSettingsRequest;
import com.hms.appointment.dto.SchedulingSettingsResponse;
import com.hms.appointment.service.SchedulingSettingsService;
import com.hms.common.api.ApiResponse;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/appointments/settings")
@RequiredArgsConstructor
public class SchedulingSettingsController {

	private final SchedulingSettingsService service;

	@GetMapping
	@PreAuthorize("hasAuthority('SETTINGS_MANAGE')")
	public ApiResponse<SchedulingSettingsResponse> get() {
		return ApiResponse.ok(service.get());
	}

	@PutMapping
	@PreAuthorize("hasAuthority('SETTINGS_MANAGE')")
	public ApiResponse<SchedulingSettingsResponse> update(@RequestBody SchedulingSettingsRequest req) {
		return ApiResponse.ok("Settings saved", service.update(req));
	}
}
