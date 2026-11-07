package com.hms.appointment.controller;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.hms.appointment.dto.AppointmentResponse;
import com.hms.appointment.dto.BookAppointmentRequest;
import com.hms.appointment.dto.CancelRequest;
import com.hms.appointment.dto.ReminderResponse;
import com.hms.appointment.dto.RescheduleRequest;
import com.hms.appointment.dto.StatusRequest;
import com.hms.appointment.enums.AppointmentStatus;
import com.hms.appointment.service.AppointmentService;
import com.hms.appointment.service.ReminderService;
import com.hms.common.api.ApiResponse;
import com.hms.common.api.PageResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/appointments")
@RequiredArgsConstructor
public class AppointmentController {

	private final AppointmentService service;
	private final ReminderService reminderService;

	@PostMapping
	@PreAuthorize("hasAuthority('APPOINTMENT_CREATE')")
	public ResponseEntity<ApiResponse<AppointmentResponse>> book(@Valid @RequestBody BookAppointmentRequest req) {
		return ResponseEntity.status(HttpStatus.CREATED)
				.body(ApiResponse.created("Appointment booked", service.book(req)));
	}

	@GetMapping
	@PreAuthorize("hasAuthority('APPOINTMENT_READ')")
	public ApiResponse<PageResponse<AppointmentResponse>> search(
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
			@RequestParam(required = false) UUID providerId, @RequestParam(required = false) UUID patientId,
			@RequestParam(required = false) AppointmentStatus status, @RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "50") int size) {
		return ApiResponse.ok(service.search(date, providerId, patientId, status,
				PageRequest.of(page, Math.min(size, 200), Sort.by("appointmentDate").and(Sort.by("startTime")))));
	}

	@GetMapping("/{id}")
	@PreAuthorize("hasAuthority('APPOINTMENT_READ')")
	public ApiResponse<AppointmentResponse> get(@PathVariable UUID id) {
		return ApiResponse.ok(service.get(id));
	}

	@PatchMapping("/{id}/status")
	@PreAuthorize("hasAuthority('APPOINTMENT_UPDATE')")
	public ApiResponse<AppointmentResponse> status(@PathVariable UUID id, @Valid @RequestBody StatusRequest req) {
		return ApiResponse.ok("Status updated", service.changeStatus(id, req.status()));
	}

	@PostMapping("/{id}/cancel")
	@PreAuthorize("hasAuthority('APPOINTMENT_CANCEL')")
	public ApiResponse<AppointmentResponse> cancel(@PathVariable UUID id, @Valid @RequestBody CancelRequest req) {
		return ApiResponse.ok("Appointment cancelled", service.cancel(id, req.reason()));
	}

	@PostMapping("/{id}/reschedule")
	@PreAuthorize("hasAuthority('APPOINTMENT_UPDATE')")
	public ApiResponse<AppointmentResponse> reschedule(@PathVariable UUID id,
			@Valid @RequestBody RescheduleRequest req) {
		return ApiResponse.ok("Appointment rescheduled", service.reschedule(id, req));
	}

	@GetMapping("/{id}/reminders")
	@PreAuthorize("hasAuthority('APPOINTMENT_READ')")
	public ApiResponse<List<ReminderResponse>> reminders(@PathVariable UUID id) {
		service.get(id); // clinic check
		return ApiResponse.ok(reminderService.list(id));
	}
}