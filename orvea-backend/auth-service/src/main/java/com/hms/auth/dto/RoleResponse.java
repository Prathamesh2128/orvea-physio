package com.hms.auth.dto;

import java.util.List;

import com.hms.common.enums.PermissionType;
import com.hms.common.enums.RoleType;

public record RoleResponse(RoleType name, String description, List<PermissionType> permissions) {

}
