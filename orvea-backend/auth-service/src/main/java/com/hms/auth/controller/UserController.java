package com.hms.auth.controller;

import java.util.UUID;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.hms.auth.dto.AssignRolesRequest;
import com.hms.auth.dto.CreateUserRequest;
import com.hms.auth.dto.UpdateUserRequest;
import com.hms.auth.dto.UserResponse;
import com.hms.auth.service.UserService;
import com.hms.common.api.ApiResponse;
import com.hms.common.api.PageResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

	private final UserService userService;

	@PostMapping
	@PreAuthorize("hasAuthority('USER_CREATE')")
	public ResponseEntity<ApiResponse<UserResponse>> crate(@Valid @RequestBody CreateUserRequest req) {
		UserResponse data = userService.create(req);
		return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created("User created", data));
	}

	@GetMapping("/{id}")
	@PreAuthorize("hasAuthority('USER_READ')")
	public ApiResponse<UserResponse> get(@PathVariable UUID id) {
		return ApiResponse.ok(userService.get(id));
	}

	@GetMapping
	@PreAuthorize("hasAuthority('USER_READ')")
	public ApiResponse<PageResponse<UserResponse>> list(@RequestParam(required = false) String search,
			@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
		return ApiResponse.ok(
				userService.list(search, PageRequest.of(page, Math.min(size, 100), Sort.by("createdAt").descending())));
	}

	@PutMapping("/{id}")
	@PreAuthorize("hasAuthority('USER_UPDATE')")
	public ApiResponse<UserResponse> update(@PathVariable UUID id, @Valid @RequestBody UpdateUserRequest req) {
		return ApiResponse.ok("User updated", userService.update(id, req));
	}

	@PutMapping("/{id}/roles")
	@PreAuthorize("hasAuthority('ROLE_MANAGE') or hasAuthority('USER_UPDATE')")
	public ApiResponse<UserResponse> assignRoles(@PathVariable UUID id, @Valid @RequestBody AssignRolesRequest req) {
		return ApiResponse.ok("Roles update", userService.assignRoles(id, req.roles()));
	}

	@DeleteMapping("/{id}")
	@PreAuthorize("hasAuthority('USER_DELETE')")
	public ApiResponse<Void> delete(@PathVariable UUID id) {
		userService.delete(id);
		return ApiResponse.ok("User deleted", null);
	}

	@PostMapping("/{id}/force-logout")
	@PreAuthorize("hasAuthority('SESSION_MANAGE')")
	public ApiResponse<Void> forceLogout(@PathVariable UUID id) {
		userService.forceLogout(id);
		return ApiResponse.ok("All sessions revoked", null);
	}
}
