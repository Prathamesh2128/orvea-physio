package com.hms.assessment.controller;

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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.hms.assessment.dto.AddendumRequest;
import com.hms.assessment.dto.AssessmentResponse;
import com.hms.assessment.dto.AssessmentSummary;
import com.hms.assessment.dto.CreateAssessmentRequest;
import com.hms.assessment.dto.SaveAnswersRequest;
import com.hms.assessment.enums.AssessmentStatus;
import com.hms.assessment.service.AssessmentService;
import com.hms.common.api.ApiResponse;
import com.hms.common.api.PageResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/assessments")
@RequiredArgsConstructor
public class AssessmentController {

	private final AssessmentService service;

	@PostMapping
	@PreAuthorize("hasAuthority('ASSESSMENT_CREATE')")
	public ResponseEntity<ApiResponse<AssessmentResponse>> create(@Valid @RequestBody CreateAssessmentRequest req) {
		return ResponseEntity.status(HttpStatus.CREATED)
				.body(ApiResponse.created("Assessment started", service.create(req)));
	}

	@GetMapping
	@PreAuthorize("hasAuthority('ASSESSMENT_READ')")
	public ApiResponse<PageResponse<AssessmentSummary>> search(@RequestParam(required = false) UUID patientId,
			@RequestParam(required = false) UUID caseId, @RequestParam(required = false) AssessmentStatus status,
			@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
		return ApiResponse.ok(service.search(patientId, caseId, status,
				PageRequest.of(page, Math.min(size, 100), Sort.by("visitDate").descending())));
	}

	@GetMapping("/{id}")
	@PreAuthorize("hasAuthority('ASSESSMENT_READ')")
	public ApiResponse<AssessmentResponse> get(@PathVariable UUID id) {
		return ApiResponse.ok(service.get(id));
	}

	/** Autosave: send only the answers that changed. */
	@PatchMapping("/{id}/answers")
	@PreAuthorize("hasAuthority('ASSESSMENT_UPDATE')")
	public ApiResponse<AssessmentResponse> saveAnswers(@PathVariable UUID id,
			@Valid @RequestBody SaveAnswersRequest req) {
		return ApiResponse.ok("Draft saved", service.saveAnswers(id, req.answers()));
	}

	@PostMapping("/{id}/complete")
	@PreAuthorize("hasAuthority('ASSESSMENT_UPDATE')")
	public ApiResponse<AssessmentResponse> complete(@PathVariable UUID id) {
		return ApiResponse.ok("Assessment completed", service.complete(id));
	}

	@PostMapping("/{id}/addenda")
	@PreAuthorize("hasAuthority('ASSESSMENT_UPDATE')")
	public ApiResponse<AssessmentResponse> addendum(@PathVariable UUID id, @Valid @RequestBody AddendumRequest req) {
		return ApiResponse.ok("Addendum added", service.addAddendum(id, req.text()));
	}

	@DeleteMapping("/{id}")
	@PreAuthorize("hasAuthority('ASSESSMENT_UPDATE')")
	public ApiResponse<Void> deleteDraft(@PathVariable UUID id) {
		service.deleteDraft(id);
		return ApiResponse.ok("Draft deleted", null);
	}
}