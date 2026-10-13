package com.hms.auth.dto;

import java.util.Set;

import com.hms.common.enums.RoleType;

import jakarta.validation.constraints.NotEmpty;

public record AssignRolesRequest(@NotEmpty Set<RoleType> roles) {

}
