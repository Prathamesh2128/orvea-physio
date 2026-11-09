package com.hms.measurement.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.hms.common.api.ApiResponse;
import com.hms.measurement.dto.SettingsDtos.SettingsRequest;
import com.hms.measurement.dto.SettingsDtos.SettingsResponse;
import com.hms.measurement.service.SettingsService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/measurements/settings")
@RequiredArgsConstructor
public class SettingsController {

	private final SettingsService service;

	@GetMapping
	@PreAuthorize("hasAnyAuthority('MEASUREMENT_SETTINGS','MEASUREMENT_PLAN')")
	public ApiResponse<SettingsResponse> get() {
		return ApiResponse.ok(service.get());
	}

	@PutMapping
	@PreAuthorize("hasAuthority('MEASUREMENT_SETTINGS')")
	public ApiResponse<SettingsResponse> update(@RequestBody SettingsRequest req) {
		return ApiResponse.ok("Settings saved", service.update(req));
	}
}