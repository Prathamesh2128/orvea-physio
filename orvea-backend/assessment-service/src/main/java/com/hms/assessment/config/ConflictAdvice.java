package com.hms.assessment.config;

import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.hms.common.api.ApiResponse;

import jakarta.servlet.http.HttpServletRequest;

@RestControllerAdvice
public class ConflictAdvice {

	@ExceptionHandler(OptimisticLockingFailureException.class)
	public ResponseEntity<ApiResponse<Void>> conflict(HttpServletRequest req) {
		return ResponseEntity.status(409).body(ApiResponse
				.error("This assessment was changed by someone else. Reload it and try again.", req.getRequestURI()));
	}
}
