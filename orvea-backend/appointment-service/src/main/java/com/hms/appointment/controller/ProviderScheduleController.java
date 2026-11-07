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
import org.springframework.web.bind.annotation.RestController;

import com.hms.appointment.dto.LeaveRequest;
import com.hms.appointment.dto.LeaveResponse;
import com.hms.appointment.dto.ProviderSettingsRequest;
import com.hms.appointment.dto.ProviderSettingsResponse;
import com.hms.appointment.dto.WorkingHoursRequest;
import com.hms.appointment.dto.WorkingHoursResponse;
import com.hms.appointment.service.ProviderScheduleService;
import com.hms.common.api.ApiResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/appointments/providers/{providerId}")
@RequiredArgsConstructor
public class ProviderScheduleController {

	private final ProviderScheduleService service;

	@GetMapping("/settings")
	@PreAuthorize("hasAnyAuthority('SETTINGS_MANAGE','APPOINTMENT_READ')")
	public ApiResponse<ProviderSettingsResponse> settings(@PathVariable UUID providerId) {
		return ApiResponse.ok(service.getSettings(providerId));
	}

	@PutMapping("/settings")
	@PreAuthorize("hasAuthority('SETTINGS_MANAGE')")
	public ApiResponse<ProviderSettingsResponse> upsertSettings(@PathVariable UUID providerId,
			@Valid @RequestBody ProviderSettingsRequest req) {
		return ApiResponse.ok("Provider settings saved", service.upsertSettings(providerId, req));
	}

	@GetMapping("/working-hours")
	@PreAuthorize("hasAnyAuthority('SETTINGS_MANAGE','APPOINTMENT_READ')")
	public ApiResponse<List<WorkingHoursResponse>> hours(@PathVariable UUID providerId) {
		return ApiResponse.ok(service.getWorkingHours(providerId));
	}

	/**
	 * Replaces the whole weekly template. Send every working block (split shifts =
	 * two rows).
	 */
	@PutMapping("/working-hours")
	@PreAuthorize("hasAuthority('SETTINGS_MANAGE')")
	public ApiResponse<List<WorkingHoursResponse>> replaceHours(@PathVariable UUID providerId,
			@Valid @RequestBody List<@Valid WorkingHoursRequest> rows) {
		return ApiResponse.ok("Working hours saved", service.replaceWorkingHours(providerId, rows));
	}

	@GetMapping("/leaves")
	@PreAuthorize("hasAnyAuthority('SETTINGS_MANAGE','APPOINTMENT_READ')")
	public ApiResponse<List<LeaveResponse>> leaves(@PathVariable UUID providerId) {
		return ApiResponse.ok(service.listLeaves(providerId));
	}

	@PostMapping("/leaves")
	@PreAuthorize("hasAuthority('SETTINGS_MANAGE')")
	public ResponseEntity<ApiResponse<LeaveResponse>> addLeave(@PathVariable UUID providerId,
			@Valid @RequestBody LeaveRequest req) {
		return ResponseEntity.status(HttpStatus.CREATED)
				.body(ApiResponse.created("Leave added", service.addLeave(providerId, req)));
	}

	@DeleteMapping("/leaves/{leaveId}")
	@PreAuthorize("hasAuthority('SETTINGS_MANAGE')")
	public ApiResponse<Void> deleteLeave(@PathVariable UUID providerId, @PathVariable UUID leaveId) {
		service.deleteLeave(providerId, leaveId);
		return ApiResponse.ok("Leave removed", null);
	}
}
