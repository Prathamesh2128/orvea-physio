package com.hms.common.security;

import java.util.List;
import java.util.UUID;

public record AuthenticatedUser(UUID userId, String username, UUID clinicId, String sessionId, List<String> roles,
		List<String> permissions) {
}
