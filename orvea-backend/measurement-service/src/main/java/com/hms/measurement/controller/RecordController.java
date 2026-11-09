package com.hms.measurement.controller;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
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
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.hms.common.api.ApiResponse;
import com.hms.common.exception.BusinessException;
import com.hms.measurement.dto.PlanDtos.ReasonRequest;
import com.hms.measurement.dto.RecordDtos.CompareResponse;
import com.hms.measurement.dto.RecordDtos.HistoryPoint;
import com.hms.measurement.dto.RecordDtos.RecordMeasurementRequest;
import com.hms.measurement.dto.RecordDtos.RecordResponse;
import com.hms.measurement.service.MeasurementService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/measurements")
@RequiredArgsConstructor
public class RecordController {

	private final MeasurementService service;

	@PostMapping("/plans/{planId}/records")
	@PreAuthorize("hasAuthority('MEASUREMENT_RECORD')")
	public ResponseEntity<ApiResponse<RecordResponse>> record(@PathVariable UUID planId,
			@Valid @RequestBody RecordMeasurementRequest req) {
		return ResponseEntity.status(HttpStatus.CREATED)
				.body(ApiResponse.created("Measurement saved", service.record(planId, req)));
	}

	/** Chart data: oldest first. */
	@GetMapping("/plans/{planId}/records")
	@PreAuthorize("hasAuthority('MEASUREMENT_READ')")
	public ApiResponse<List<HistoryPoint>> history(@PathVariable UUID planId) {
		return ApiResponse.ok(service.history(planId));
	}

	/**
	 * Before vs after. Without parameters: first reading of the current protocol vs
	 * the latest.
	 */
	@GetMapping("/plans/{planId}/compare")
	@PreAuthorize("hasAuthority('MEASUREMENT_READ')")
	public ApiResponse<CompareResponse> compare(@PathVariable UUID planId,
			@RequestParam(required = false) UUID baselineId, @RequestParam(required = false) UUID targetId) {
		return ApiResponse.ok(service.compare(planId, baselineId, targetId));
	}

	@GetMapping("/records/{recordId}")
	@PreAuthorize("hasAuthority('MEASUREMENT_READ')")
	public ApiResponse<RecordResponse> get(@PathVariable UUID recordId) {
		return ApiResponse.ok(service.get(recordId));
	}

	@PostMapping("/records/{recordId}/void")
	@PreAuthorize("hasAuthority('MEASUREMENT_PLAN')")
	public ApiResponse<RecordResponse> voidRecord(@PathVariable UUID recordId, @Valid @RequestBody ReasonRequest req) {
		return ApiResponse.ok("Measurement voided", service.voidRecord(recordId, req.reason()));
	}

	@PutMapping(path = "/records/{recordId}/photo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	@PreAuthorize("hasAuthority('MEASUREMENT_RECORD')")
	public ApiResponse<RecordResponse> uploadPhoto(@PathVariable UUID recordId, @RequestPart("file") MultipartFile file)
			throws IOException {
		if (file.isEmpty())
			throw new BusinessException("No file was sent");
		return ApiResponse.ok("Photo stored", service.uploadPhoto(recordId, file.getBytes()));
	}

	@GetMapping("/records/{recordId}/photo")
	@PreAuthorize("hasAuthority('MEASUREMENT_READ')")
	public ResponseEntity<byte[]> photo(@PathVariable UUID recordId) {
		MeasurementService.PhotoData p = service.getPhoto(recordId);
		return ResponseEntity.ok().contentType(MediaType.parseMediaType(p.contentType()))
				.cacheControl(CacheControl.noStore()).header("X-Content-Type-Options", "nosniff").body(p.bytes());
	}

	@DeleteMapping("/records/{recordId}/photo")
	@PreAuthorize("hasAuthority('MEASUREMENT_RECORD')")
	public ApiResponse<RecordResponse> deletePhoto(@PathVariable UUID recordId) {
		return ApiResponse.ok("Photo deleted", service.deletePhoto(recordId));
	}
}