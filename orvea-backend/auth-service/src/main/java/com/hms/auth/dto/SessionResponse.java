package com.hms.auth.dto;

import java.time.LocalDateTime;
import java.util.UUID;

import com.hms.auth.enums.SessionStatus;

public record SessionResponse(UUID id, String ipAddress, String userAgent, LocalDateTime loginAt,
		LocalDateTime lastAccessAt, LocalDateTime expiresAt, SessionStatus status, boolean current) {

}
