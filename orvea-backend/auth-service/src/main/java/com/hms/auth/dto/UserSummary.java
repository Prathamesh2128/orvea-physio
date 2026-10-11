package com.hms.auth.dto;

import java.util.List;
import java.util.UUID;

import com.hms.common.enums.RoleType;

public record UserSummary(UUID ID, String username, String fullName, List<RoleType> roles, List<String> permissions,
		boolean mustChangePassword) {
}
