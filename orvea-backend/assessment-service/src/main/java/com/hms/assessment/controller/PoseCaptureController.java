package com.hms.assessment.controller;

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
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.hms.assessment.dto.CaptureRequest;
import com.hms.assessment.dto.CaptureResponse;
import com.hms.assessment.dto.ConfirmCaptureRequest;
import com.hms.assessment.dto.RejectCaptureRequest;
import com.hms.assessment.service.PoseCaptureService;
import com.hms.common.api.ApiResponse;
import com.hms.common.exception.BusinessException;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/assessments/{assessmentId}/captures")
@RequiredArgsConstructor
public class PoseCaptureController {

	private final PoseCaptureService service;

	@PostMapping
	@PreAuthorize("hasAuthority('ASSESSMENT_UPDATE')")
	public ResponseEntity<ApiResponse<CaptureResponse>> create(@PathVariable UUID assessmentId,
			@Valid @RequestBody CaptureRequest req) {
		return ResponseEntity.status(HttpStatus.CREATED)
				.body(ApiResponse.created("Capture saved as a suggestion", service.create(assessmentId, req)));
	}

	@GetMapping
	@PreAuthorize("hasAuthority('ASSESSMENT_READ')")
	public ApiResponse<List<CaptureResponse>> list(@PathVariable UUID assessmentId) {
		return ApiResponse.ok(service.list(assessmentId));
	}

	@PostMapping("/{captureId}/confirm")
	@PreAuthorize("hasAuthority('ASSESSMENT_UPDATE')")
	public ApiResponse<CaptureResponse> confirm(@PathVariable UUID assessmentId, @PathVariable UUID captureId,
			@Valid @RequestBody(required = false) ConfirmCaptureRequest req) {
		return ApiResponse.ok("Capture confirmed", service.confirm(assessmentId, captureId, req));
	}

	@PostMapping("/{captureId}/reject")
	@PreAuthorize("hasAuthority('ASSESSMENT_UPDATE')")
	public ApiResponse<CaptureResponse> reject(@PathVariable UUID assessmentId, @PathVariable UUID captureId,
			@Valid @RequestBody RejectCaptureRequest req) {
		return ApiResponse.ok("Capture rejected", service.reject(assessmentId, captureId, req.reason()));
	}

	@PutMapping(path = "/{captureId}/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	@PreAuthorize("hasAuthority('ASSESSMENT_UPDATE')")
	public ApiResponse<CaptureResponse> uploadImage(@PathVariable UUID assessmentId, @PathVariable UUID captureId,
			@RequestPart("file") MultipartFile file) throws IOException {
		if (file.isEmpty())
			throw new BusinessException("No file was sent");
		return ApiResponse.ok("Image stored", service.uploadImage(assessmentId, captureId, file.getBytes()));
	}

	@GetMapping("/{captureId}/image")
	@PreAuthorize("hasAuthority('ASSESSMENT_READ')")
	public ResponseEntity<byte[]> image(@PathVariable UUID assessmentId, @PathVariable UUID captureId) {
		PoseCaptureService.ImageData img = service.getImage(assessmentId, captureId);
		return ResponseEntity.ok().contentType(MediaType.parseMediaType(img.contentType()))
				.cacheControl(CacheControl.noStore()).header("X-Content-Type-Options", "nosniff").body(img.bytes());
	}

	@DeleteMapping("/{captureId}/image")
	@PreAuthorize("hasAuthority('ASSESSMENT_UPDATE')")
	public ApiResponse<CaptureResponse> deleteImage(@PathVariable UUID assessmentId, @PathVariable UUID captureId) {
		return ApiResponse.ok("Image deleted", service.deleteImage(assessmentId, captureId));
	}
}