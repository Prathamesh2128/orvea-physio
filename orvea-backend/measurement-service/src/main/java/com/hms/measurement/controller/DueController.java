package com.hms.measurement.controller;

import java.util.UUID;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.hms.common.api.ApiResponse;
import com.hms.measurement.dto.DueDtos.DueList;
import com.hms.measurement.service.DueService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/measurements/due")
@RequiredArgsConstructor
public class DueController {

	private final DueService service;

	/**
	 * therapistId (staff id) filters to one therapist; omit for the whole clinic.
	 */
	@GetMapping
	@PreAuthorize("hasAuthority('MEASUREMENT_READ')")
	public ApiResponse<DueList> due(@RequestParam(required = false) UUID therapistId) {
		return ApiResponse.ok(service.due(therapistId));
	}
}