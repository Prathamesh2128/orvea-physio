package com.hms.auth.dto;

import java.util.Set;
import java.util.UUID;

import com.hms.common.enums.RoleType;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateUserRequest(
		@NotBlank @Size(min = 3, max = 60) @Pattern(regexp = "^[A-Za-z0-9._-]+$") String username,
		@NotBlank @Email String email, @Size(min = 10, max = 12) String phone,
		@NotBlank @Size(min = 3, max = 20) String firstName, @NotBlank @Size(min = 3, max = 20) String lastName,
		@NotBlank @Pattern(regexp = "^(?=.*[A-Z])(?=.*[a-z])(?=.*\\d)(?=.*[^A-Za-z0-9]).{8,64}$", message = "Password must be 8-64 chars with upper, lower, digit and special character") String password,
		@NotEmpty Set<RoleType> roles, UUID clinicId) {

}
