package com.hms.audit.controller;

import java.time.LocalDate;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.hms.audit.dato.AuditLogResponse;
import com.hms.audit.service.AuditQueryService;
import com.hms.common.api.ApiResponse;
import com.hms.common.api.PageResponse;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/audit")
@PreAuthorize("hasAuthority('AUDIT_VIEW')")
@RequiredArgsConstructor
public class AuditController {

	private final AuditQueryService service;

	/** Read-only. There is deliberately no update or delete endpoint. */
	@GetMapping
	public ApiResponse<PageResponse<AuditLogResponse>> search(@RequestParam(required = false) String entityType,
			@RequestParam(required = false) String entityId, @RequestParam(required = false) String actor,
			@RequestParam(required = false) String action, @RequestParam(required = false) String source,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
			@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "50") int size) {
		return ApiResponse.ok(service.search(entityType, entityId, actor, action, source, from, to,
				PageRequest.of(page, Math.min(size, 200), Sort.by("occurredAt").descending())));
	}
}