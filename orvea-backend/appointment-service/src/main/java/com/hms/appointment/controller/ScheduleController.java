package com.hms.appointment.controller;

import java.time.LocalDate;
import java.util.UUID;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.hms.appointment.dto.AvailabilityResponse;
import com.hms.appointment.dto.DayScheduleResponse;
import com.hms.appointment.service.AvailabilityService;
import com.hms.common.api.ApiResponse;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/appointments")
@RequiredArgsConstructor
public class ScheduleController {

	private final AvailabilityService service;

	/**
	 * Daily schedule for one therapist (booked / available / lunch / leave cells).
	 */
	@GetMapping("/providers/{providerId}/schedule")
	@PreAuthorize("hasAnyAuthority('APPOINTMENT_READ','APPOINTMENT_CREATE')")
	public ApiResponse<DayScheduleResponse> day(@PathVariable UUID providerId,
			@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
		return ApiResponse.ok(service.daySchedule(providerId, date));
	}

	/**
	 * Free start times for the chosen visit type (feeds the phone-booking form).
	 */
	@GetMapping("/availability")
	@PreAuthorize("hasAnyAuthority('APPOINTMENT_READ','APPOINTMENT_CREATE')")
	public ApiResponse<AvailabilityResponse> availability(@RequestParam UUID providerId,
			@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
			@RequestParam UUID visitTypeId) {
		return ApiResponse.ok(service.availability(providerId, date, visitTypeId));
	}
}
