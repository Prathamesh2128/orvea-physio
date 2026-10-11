package com.hms.auth.dto;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

import com.hms.common.enums.RoleType;

public record UserResponse(UUID id, String username, String email, String phone, String firstName, String lastName,
		boolean enabled, Set<RoleType> roles, LocalDateTime lastLoginAt, LocalDateTime createdAt) {

}
