package com.hms.patient.controller;

import java.util.List;
import java.util.UUID;

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
import org.springframework.web.bind.annotation.RestController;

import com.hms.common.api.ApiResponse;
import com.hms.patient.dto.CaseRequest;
import com.hms.patient.dto.CaseResponse;
import com.hms.patient.dto.CaseStatusRequest;
import com.hms.patient.dto.ConditionRequest;
import com.hms.patient.service.TreatmentCaseService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("aoi/v1/patients/{patientId}/cases")
public class TreatmentCaseController {

	private final TreatmentCaseService treatmentCaseService;

	@PostMapping
	@PreAuthorize("hasAuthority('TREATMENT_CREATE')")
	public ResponseEntity<ApiResponse<CaseResponse>> create(@PathVariable UUID patientId,
			@Valid @RequestBody CaseRequest req) {
		return ResponseEntity.status(HttpStatus.CREATED)
				.body(ApiResponse.created("Case created", treatmentCaseService.create(patientId, req)));
	}

	@GetMapping
	@PreAuthorize("hasAuthority('TREATMENT_READ')")
	public ApiResponse<List<CaseResponse>> list(@PathVariable UUID patientId) {
		return ApiResponse.ok(treatmentCaseService.list(patientId));
	}

	@GetMapping("/{caseId}")
	@PreAuthorize("hasAuthority('TREATMENT_READ')")
	public ApiResponse<CaseResponse> get(@PathVariable UUID patientId, @PathVariable UUID caseId) {
		return ApiResponse.ok(treatmentCaseService.get(patientId, caseId));
	}

	@PutMapping("/{caseId}")
	@PreAuthorize("hasAuthority('TREATMENT_UPDATE')")
	public ApiResponse<CaseResponse> update(@PathVariable UUID patientId, @PathVariable UUID caseId,
			@Valid @RequestBody CaseRequest req) {
		return ApiResponse.ok("Case updated", treatmentCaseService.update(patientId, caseId, req));
	}

	@PatchMapping("/{caseId}/status")
	@PreAuthorize("hasAuthority('TREATMENT_UPDATE')")
	public ApiResponse<CaseResponse> status(@PathVariable UUID patientId, @PathVariable UUID caseId,
			@Valid @RequestBody CaseStatusRequest req) {
		return ApiResponse.ok("Case status updated", treatmentCaseService.changeStatus(patientId, caseId, req));
	}

	@PostMapping("/{caseId}/sessions/complete")
	@PreAuthorize("hasAuthority('TREATMENT_UPDATE')")
	public ApiResponse<CaseResponse> sessionCompleted(@PathVariable UUID patientId, @PathVariable UUID caseId) {
		return ApiResponse.ok("Session recorded", treatmentCaseService.recordSessionCompleted(patientId, caseId));
	}

	@DeleteMapping("/{caseId}")
	@PreAuthorize("hasAuthority('TREATMENT_UPDATE')")
	public ApiResponse<Void> delete(@PathVariable UUID patientId, @PathVariable UUID caseId) {
		treatmentCaseService.delete(patientId, caseId);
		return ApiResponse.ok("Case deleted", null);
	}

	@PostMapping("/{caseId}/conditions")
	@PreAuthorize("hasAuthority('TREATMENT_UPDATE')")
	public ApiResponse<CaseResponse> addCondition(@PathVariable UUID patientId, @PathVariable UUID caseId,
			@Valid @RequestBody ConditionRequest req) {
		return ApiResponse.ok("Condition added", treatmentCaseService.addCondition(patientId, caseId, req));
	}

	@DeleteMapping("/{caseId}/conditions/{conditionId}")
	@PreAuthorize("hasAuthority('TREATMENT_UPDATE')")
	public ApiResponse<CaseResponse> removeCondition(@PathVariable UUID patientId, @PathVariable UUID caseId,
			@PathVariable UUID conditionId) {
		return ApiResponse.ok("Condition removed",
				treatmentCaseService.removeCondition(patientId, caseId, conditionId));
	}
}
