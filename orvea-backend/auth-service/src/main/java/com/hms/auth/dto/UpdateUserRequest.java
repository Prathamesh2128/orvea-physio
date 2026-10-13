package com.hms.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateUserRequest(@Email String email, @Size(min = 10, max = 12) String phone,
		@NotBlank @Size(max = 20) String firstName, @NotBlank @Size(max = 20) String lastName, Boolean enabled) {

}
