package com.hms.staff.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.hms.common.api.ApiResponse;
import com.hms.common.api.PageResponse;
import com.hms.common.enums.RoleType;
import com.hms.staff.dto.StaffRequest;
import com.hms.staff.dto.StaffResponse;
import com.hms.staff.enums.StaffStatus;
import com.hms.staff.service.StaffService;
import com.hms.staff.utill.StaffStatusRequest;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/staff")
@RequiredArgsConstructor
public class StaffController {

	private final StaffService service;

	@PostMapping
	@PreAuthorize("hasAuthority('STAFF_CREATE')")
	public ResponseEntity<ApiResponse<StaffResponse>> create(@Valid @RequestBody StaffRequest req) {
		return ResponseEntity.status(HttpStatus.CREATED)
				.body(ApiResponse.created("Staff created", service.create(req)));
	}

	@GetMapping
	@PreAuthorize("hasAuthority('STAFF_READ')")
	public ApiResponse<PageResponse<StaffResponse>> search(@RequestParam(required = false) RoleType type,
			@RequestParam(required = false) StaffStatus status, @RequestParam(required = false) String q,
			@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
		return ApiResponse
				.ok(service.search(type, status, q, PageRequest.of(page, Math.min(size, 100), Sort.by("displayName"))));
	}

	/**
	 * Dropdown of active physiotherapists/doctors; receptionists need it for phone
	 * booking.
	 */
	@GetMapping("/treating")
	@PreAuthorize("hasAnyAuthority('STAFF_READ','APPOINTMENT_CREATE')")
	public ApiResponse<List<StaffResponse>> treating() {
		return ApiResponse.ok(service.treatingStaff());
	}

	@GetMapping("/{id}")
	@PreAuthorize("hasAuthority('STAFF_READ')")
	public ApiResponse<StaffResponse> get(@PathVariable UUID id) {
		return ApiResponse.ok(service.get(id));
	}

	@PutMapping("/{id}")
	@PreAuthorize("hasAuthority('STAFF_UPDATE')")
	public ApiResponse<StaffResponse> update(@PathVariable UUID id, @Valid @RequestBody StaffRequest req) {
		return ApiResponse.ok("Staff updated", service.update(id, req));
	}

	@PatchMapping("/{id}/status")
	@PreAuthorize("hasAuthority('STAFF_UPDATE')")
	public ApiResponse<StaffResponse> status(@PathVariable UUID id, @Valid @RequestBody StaffStatusRequest req) {
		return ApiResponse.ok("Status updated", service.setStatus(id, req.status()));
	}

	@DeleteMapping("/{id}")
	@PreAuthorize("hasAuthority('STAFF_DELETE')")
	public ApiResponse<Void> delete(@PathVariable UUID id) {
		service.delete(id);
		return ApiResponse.ok("Staff deleted", null);
	}
}
