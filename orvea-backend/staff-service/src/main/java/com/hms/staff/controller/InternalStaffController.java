package com.hms.staff.controller;

import java.util.UUID;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.hms.common.api.ApiResponse;
import com.hms.staff.dto.StaffRef;
import com.hms.staff.service.StaffService;

import lombok.RequiredArgsConstructor;

/** Service-to-service only. The gateway does not route /api/v1/internal/**. */
@RestController
@RequestMapping("/api/v1/internal/staff")
@RequiredArgsConstructor
public class InternalStaffController {

	private final StaffService service;

	@GetMapping("/{id}")
	@PreAuthorize("isAuthenticated()")
	public ApiResponse<StaffRef> ref(@PathVariable UUID id) {
		return ApiResponse.ok(service.internalRef(id));
	}
}
