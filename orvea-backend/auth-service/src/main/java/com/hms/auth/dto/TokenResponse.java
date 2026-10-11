package com.hms.auth.dto;

public record TokenResponse(String accessToke, String refreshToken, String tokenType, long expiresIn, String sessionId,
		UserSummary user) {
}
