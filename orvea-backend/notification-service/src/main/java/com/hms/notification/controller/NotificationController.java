package com.hms.notification.controller;

import java.util.UUID;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.hms.common.api.ApiResponse;
import com.hms.common.api.PageResponse;
import com.hms.notification.dto.NotificationResponse;
import com.hms.notification.enums.MessageType;
import com.hms.notification.enums.NotificationStatus;
import com.hms.notification.service.NotificationService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/notifications")
@PreAuthorize("hasAuthority('NOTIFICATION_SEND')")
@RequiredArgsConstructor
public class NotificationController {

	private final NotificationService service;

	/** The outbox. Receptionist's daily list: status=READY_TO_SEND. */
	@GetMapping
	public ApiResponse<PageResponse<NotificationResponse>> list(
			@RequestParam(required = false) NotificationStatus status, @RequestParam(required = false) MessageType type,
			@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "50") int size) {
		return ApiResponse.ok(service.search(status, type,
				PageRequest.of(page, Math.min(size, 200), Sort.by("createdAt").descending())));
	}

	@GetMapping("/{id}")
	public ApiResponse<NotificationResponse> get(@PathVariable UUID id) {
		return ApiResponse.ok(service.get(id));
	}

	@PostMapping("/{id}/mark-sent")
	public ApiResponse<NotificationResponse> markSent(@PathVariable UUID id) {
		return ApiResponse.ok("Marked as sent", service.markSent(id));
	}

	@PostMapping("/{id}/cancel")
	public ApiResponse<NotificationResponse> cancel(@PathVariable UUID id) {
		return ApiResponse.ok("Message cancelled", service.cancel(id));
	}

	@PostMapping("/{id}/retry")
	public ApiResponse<NotificationResponse> retry(@PathVariable UUID id) {
		return ApiResponse.ok("Message re-queued", service.retry(id));
	}
}
