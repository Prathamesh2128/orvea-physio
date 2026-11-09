package com.hms.measurement.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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
import com.hms.measurement.dto.PlanDtos.ChangeProtocolRequest;
import com.hms.measurement.dto.PlanDtos.CreatePlanRequest;
import com.hms.measurement.dto.PlanDtos.EventView;
import com.hms.measurement.dto.PlanDtos.PlanForm;
import com.hms.measurement.dto.PlanDtos.PlanResponse;
import com.hms.measurement.dto.PlanDtos.ReasonRequest;
import com.hms.measurement.dto.PlanDtos.SnoozeRequest;
import com.hms.measurement.dto.PlanDtos.UpdatePlanRequest;
import com.hms.measurement.enums.PlanStatus;
import com.hms.measurement.service.PlanService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/measurements/plans")
@RequiredArgsConstructor
public class PlanController {

	private final PlanService service;

	@PostMapping
	@PreAuthorize("hasAuthority('MEASUREMENT_PLAN')")
	public ResponseEntity<ApiResponse<PlanResponse>> create(@Valid @RequestBody CreatePlanRequest req) {
		return ResponseEntity.status(HttpStatus.CREATED)
				.body(ApiResponse.created("Measurement plan created", service.create(req)));
	}

	@GetMapping
	@PreAuthorize("hasAuthority('MEASUREMENT_READ')")
	public ApiResponse<PageResponse<PlanResponse>> search(@RequestParam(required = false) UUID patientId,
			@RequestParam(required = false) UUID caseId, @RequestParam(required = false) PlanStatus status,
			@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
		return ApiResponse.ok(service.search(patientId, caseId, status,
				PageRequest.of(page, Math.min(size, 100), Sort.by("nextDueDate"))));
	}

	@GetMapping("/{id}")
	@PreAuthorize("hasAuthority('MEASUREMENT_READ')")
	public ApiResponse<PlanResponse> get(@PathVariable UUID id) {
		return ApiResponse.ok(service.get(id));
	}

	/**
	 * "Measure now" opens this: the saved protocol, default side and previous
	 * values.
	 */
	@GetMapping("/{id}/form")
	@PreAuthorize("hasAuthority('MEASUREMENT_RECORD')")
	public ApiResponse<PlanForm> form(@PathVariable UUID id) {
		return ApiResponse.ok(service.form(id));
	}

	@GetMapping("/{id}/events")
	@PreAuthorize("hasAuthority('MEASUREMENT_READ')")
	public ApiResponse<List<EventView>> events(@PathVariable UUID id) {
		return ApiResponse.ok(service.history(id));
	}

	@PatchMapping("/{id}")
	@PreAuthorize("hasAuthority('MEASUREMENT_PLAN')")
	public ApiResponse<PlanResponse> update(@PathVariable UUID id, @Valid @RequestBody UpdatePlanRequest req) {
		return ApiResponse.ok("Plan updated", service.update(id, req));
	}

	/**
	 * 409 with a warning when readings already exist; resend with
	 * confirmChange=true and a reason.
	 */
	@PutMapping("/{id}/protocol")
	@PreAuthorize("hasAuthority('MEASUREMENT_PLAN')")
	public ApiResponse<PlanResponse> changeProtocol(@PathVariable UUID id,
			@Valid @RequestBody ChangeProtocolRequest req) {
		return ApiResponse.ok("Protocol updated", service.changeProtocol(id, req));
	}

	@PostMapping("/{id}/snooze")
	@PreAuthorize("hasAuthority('MEASUREMENT_PLAN')")
	public ApiResponse<PlanResponse> snooze(@PathVariable UUID id, @Valid @RequestBody SnoozeRequest req) {
		return ApiResponse.ok("Snoozed", service.snooze(id, req));
	}

	@PostMapping("/{id}/skip")
	@PreAuthorize("hasAuthority('MEASUREMENT_PLAN')")
	public ApiResponse<PlanResponse> skip(@PathVariable UUID id, @Valid @RequestBody ReasonRequest req) {
		return ApiResponse.ok("Marked as not needed", service.skip(id, req.reason()));
	}

	@PostMapping("/{id}/pause")
	@PreAuthorize("hasAuthority('MEASUREMENT_PLAN')")
	public ApiResponse<PlanResponse> pause(@PathVariable UUID id, @Valid @RequestBody ReasonRequest req) {
		return ApiResponse.ok("Plan paused", service.pause(id, req.reason()));
	}

	@PostMapping("/{id}/resume")
	@PreAuthorize("hasAuthority('MEASUREMENT_PLAN')")
	public ApiResponse<PlanResponse> resume(@PathVariable UUID id) {
		return ApiResponse.ok("Plan resumed", service.resume(id));
	}

	@PostMapping("/{id}/end")
	@PreAuthorize("hasAuthority('MEASUREMENT_PLAN')")
	public ApiResponse<PlanResponse> end(@PathVariable UUID id, @Valid @RequestBody ReasonRequest req) {
		return ApiResponse.ok("Plan ended", service.end(id, req.reason()));
	}
}