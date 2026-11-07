package com.hms.patient.controller;

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

import com.hms.common.api.ApiResponse;
import com.hms.patient.dto.CareTeamRequest;
import com.hms.patient.dto.CareTeamResponse;
import com.hms.patient.service.CareTeamService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/patients/{patientId}/care-team")
@RequiredArgsConstructor
public class CareTeamController {

	private final CareTeamService careTeamService;

	@PostMapping
	@PreAuthorize("hasAuthority('PATIENT_UPDATE')")
	public ResponseEntity<ApiResponse<CareTeamResponse>> add(@PathVariable UUID patientId,
			@Valid @RequestBody CareTeamRequest req) {
		return ResponseEntity.status(HttpStatus.CREATED)
				.body(ApiResponse.created("Care team member added", careTeamService.add(patientId, req)));
	}

	@GetMapping
	@PreAuthorize("hasAuthority('PATIENT_READ')")
	public ApiResponse<List<CareTeamResponse>> list(@PathVariable UUID patientId) {
		return ApiResponse.ok(careTeamService.list(patientId));
	}

	@PutMapping("/{memberId}")
	@PreAuthorize("hasAuthority('PATIENT_UPDATE')")
	public ApiResponse<CareTeamResponse> update(@PathVariable UUID patientId, @PathVariable UUID memberId,
			@Valid @RequestBody CareTeamRequest req) {
		return ApiResponse.ok("Care team member updated", careTeamService.update(patientId, memberId, req));
	}

	@DeleteMapping("/{memberId}")
	@PreAuthorize("hasAuthority('PATIENT_UPDATE')")
	public ApiResponse<Void> delete(@PathVariable UUID patientId, @PathVariable UUID memberId) {
		careTeamService.delete(patientId, memberId);
		return ApiResponse.ok("Care team member removed", null);
	}
}
