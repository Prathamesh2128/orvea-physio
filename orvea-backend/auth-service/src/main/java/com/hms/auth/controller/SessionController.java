package com.hms.auth.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.hms.auth.dto.SessionResponse;
import com.hms.auth.service.AuthService;
import com.hms.common.api.ApiResponse;
import com.hms.common.exception.UnauthorizedException;
import com.hms.common.security.AuthenticatedUser;
import com.hms.common.security.SecurityUtils;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/sessions")
@RequiredArgsConstructor
public class SessionController {
	private final AuthService authService;

	@GetMapping("/me")
	public ApiResponse<List<SessionResponse>> mySessions() {
		AuthenticatedUser u = current();
		return ApiResponse.ok(authService.activeSessions(u.userId(), u.sessionId()));
	}

	@DeleteMapping("/{sessionId}")
	public ApiResponse<Void> revoke(@PathVariable UUID sessionId) {
		authService.logout(current().userId(), sessionId);
		return ApiResponse.ok("Session revoked", null);
	}

	private AuthenticatedUser current() {
		return SecurityUtils.currentUser().orElseThrow(() -> new UnauthorizedException("Not authenticated"));
	}
}
