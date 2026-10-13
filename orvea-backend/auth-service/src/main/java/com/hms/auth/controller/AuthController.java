package com.hms.auth.controller;

import java.util.UUID;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.hms.auth.dto.ChangePasswordRequest;
import com.hms.auth.dto.LoginRequest;
import com.hms.auth.dto.RefreshRequest;
import com.hms.auth.dto.TokenResponse;
import com.hms.auth.dto.UserSummary;
import com.hms.auth.service.AuthService;
import com.hms.common.api.ApiResponse;
import com.hms.common.exception.UnauthorizedException;
import com.hms.common.security.AuthenticatedUser;
import com.hms.common.security.SecurityUtils;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

	private final AuthService authService;

	@PostMapping("/login")
	public ApiResponse<TokenResponse> login(@Valid @RequestBody LoginRequest req, HttpServletRequest httpReq) {
		TokenResponse data = authService.login(req, clientIp(httpReq), httpReq.getHeader("User-Agent"));

		return ApiResponse.ok("Login successfull", data);
	}

	@PostMapping("/refresh")
	public ApiResponse<TokenResponse> refresh(@Valid @RequestBody RefreshRequest req) {
		TokenResponse data = authService.refresh(req.refreshToken());
		return ApiResponse.ok(data);
	}

	@PostMapping("/logout")
	public ApiResponse<Void> logout() {
		AuthenticatedUser u = current();
		authService.logout(UUID.fromString(u.sessionId()), u.userId());
		return ApiResponse.ok("Logged out", null);
	}

	@PostMapping("/logout-all")
	public ApiResponse<Void> logoutAll() {
		authService.logoutAll(current().userId());
		return ApiResponse.ok("Logged out from all devices", null);
	}

	@GetMapping("/me")
	public ApiResponse<UserSummary> me() {
		UserSummary data = authService.me(current().userId());
		return ApiResponse.ok(data);
	}

	@PostMapping("/change-password")
	public ApiResponse<Void> changePassword(@Valid @RequestBody ChangePasswordRequest req) {
		authService.changePassword(current().userId(), req);
		return ApiResponse.ok("Password changed. Please login again.", null);
	}
	
	// Helper Methods
	private String clientIp(HttpServletRequest req) {
		String xff = req.getHeader("X-Forwarded-For");
		return xff != null && !xff.isBlank() ? xff.split(",")[0].trim() : req.getRemoteAddr();
	}

	private AuthenticatedUser current() {
		return SecurityUtils.currentUser().orElseThrow(() -> new UnauthorizedException("Not authenticated"));
	}
}
