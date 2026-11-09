package com.hms.billing.controller;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.hms.billing.dto.ReportDtos.CollectionReport;
import com.hms.billing.dto.ReportDtos.DueReport;
import com.hms.billing.dto.ReportDtos.RevenueReport;
import com.hms.billing.service.ReportService;
import com.hms.common.api.ApiResponse;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/billing")
@RequiredArgsConstructor
public class ReportController {

	private final ReportService service;

	@GetMapping("/dues")
	@PreAuthorize("hasAuthority('BILLING_READ')")
	public ApiResponse<DueReport> dues() {
		return ApiResponse.ok(service.dues());
	}

	@GetMapping("/reports/collection")
	@PreAuthorize("hasAnyAuthority('REPORT_VIEW','BILLING_UPDATE')")
	public ApiResponse<CollectionReport> collection(
			@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
			@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
		return ApiResponse.ok(service.collection(from, to));
	}

	@GetMapping("/reports/revenue")
	@PreAuthorize("hasAnyAuthority('REPORT_VIEW','BILLING_UPDATE')")
	public ApiResponse<RevenueReport> revenue(
			@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
			@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
		return ApiResponse.ok(service.revenue(from, to));
	}
}