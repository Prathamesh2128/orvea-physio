package com.hms.appointment.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.hms.appointment.dto.NextVisit;
import com.hms.appointment.service.NextVisitService;
import com.hms.common.api.ApiResponse;

import lombok.RequiredArgsConstructor;

/** Service-to-service only. The gateway does not route /api/v1/internal/**. */
@RestController
@RequestMapping("/api/v1/internal/appointments")
@RequiredArgsConstructor
public class InternalAppointmentController {

	private final NextVisitService service;

	@GetMapping("/next")
	@PreAuthorize("isAuthenticated()")
	public ApiResponse<List<NextVisit>> next(@RequestParam List<UUID> patientIds) {
		return ApiResponse.ok(service.nextVisits(patientIds));
	}
}