package com.hms.patient.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.hms.common.api.ApiResponse;
import com.hms.common.api.PageResponse;
import com.hms.common.security.ClinicScope;
import com.hms.patient.dto.DuplicateCandidate;
import com.hms.patient.dto.PatientRequest;
import com.hms.patient.dto.PatientResponse;
import com.hms.patient.dto.PatientSummary;
import com.hms.patient.service.PatientService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/patients")
@RequiredArgsConstructor
public class PatientController {

	private final PatientService patientService;

	@PostMapping
	@PreAuthorize("hasAuthority('PATIENT_CREATE')")
	public ResponseEntity<ApiResponse<PatientResponse>> register(@Valid @RequestBody PatientRequest req) {
		PatientResponse data = patientService.register(req);
		return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created("Patient Created", data));
	}

	/** Call this from the registration form before submitting. */
	@PostMapping("/duplicate-check")
	@PreAuthorize("hasAnyAuthority('PATIENT_CREATE','PATIENT_READ')")
	public ApiResponse<List<DuplicateCandidate>> duplicateCheck(@Valid @RequestBody PatientRequest req) {
		return ApiResponse.ok(patientService.findDuplicates(req, ClinicScope.currentClinicId()));
	}

	@GetMapping("/search")
	@PreAuthorize("hasAuthority('PATIENT_READ')")
	public ApiResponse<PageResponse<PatientSummary>> search(@RequestParam(required = false) String phone,
			@RequestParam(required = false) String name, @RequestParam(required = false) String uhid,
			@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
		return ApiResponse.ok(patientService.search(phone, name, uhid,
				PageRequest.of(page, Math.min(size, 100), Sort.by("firstName").ascending())));
	}

	@GetMapping("/me")
	@PreAuthorize("hasAuthority('PATIENT')")
	public ApiResponse<PatientResponse> me() {
		return ApiResponse.ok(patientService.getMine());
	}

	@GetMapping("/uhid/{uhid}")
	@PreAuthorize("hasAuthority('PATIENT_READ')")
	public ApiResponse<PatientResponse> byUhid(@PathVariable String uhid) {
		return ApiResponse.ok(patientService.getByUhid(uhid));
	}

	@GetMapping("/{id}")
	@PreAuthorize("hasAuthority('PATIENT_READ')")
	public ApiResponse<PatientResponse> get(@PathVariable UUID id) {
		return ApiResponse.ok(patientService.get(id));
	}

	@PutMapping("/{id}")
	@PreAuthorize("hasAuthority('PATIENT_UPDATE')")
	public ApiResponse<PatientResponse> update(@PathVariable UUID id, @Valid @RequestBody PatientRequest req) {
		return ApiResponse.ok("Patient updated", patientService.update(id, req));
	}

	@DeleteMapping("/{id}")
	@PreAuthorize("hasAuthority('PATIENT_DELETE')")
	public ApiResponse<Void> delete(@PathVariable UUID id) {
		patientService.delete(id);
		return ApiResponse.ok("Patient deleted", null);
	}
}
