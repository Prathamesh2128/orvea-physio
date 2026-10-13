package com.hms.auth.controller;

import java.util.Comparator;
import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.hms.auth.dto.RoleResponse;
import com.hms.auth.repository.PermissionRepository;
import com.hms.auth.repository.RoleRepository;
import com.hms.common.api.ApiResponse;
import com.hms.common.enums.PermissionType;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class RoleController {

	private final RoleRepository roleRepository;
	private final PermissionRepository permissionRepository;

	@GetMapping("/api/v1/roles")
	@PreAuthorize("hasAnyAuthority('ROLE_MANAGE','USER_READ')")
	@Transactional(readOnly = true)
	public ApiResponse<List<RoleResponse>> roles() {
		return ApiResponse.ok(roleRepository.findAll().stream()
				.map(r -> new RoleResponse(r.getName(), r.getDescription(),
						r.getPermissions().stream().map(p -> p.getName()).sorted().toList()))
				.sorted(Comparator.comparing(RoleResponse::name)).toList());
	}

	@GetMapping("/api/v1/permissions")
	@PreAuthorize("hasAuthority('PERMISSION_MANAGE')")
	public ApiResponse<List<PermissionType>> permissions() {
		return ApiResponse.ok(permissionRepository.findAll().stream().map(p -> p.getName()).sorted().toList());
	}
}
