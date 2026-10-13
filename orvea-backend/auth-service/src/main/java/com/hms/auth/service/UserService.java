package com.hms.auth.service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hms.auth.dto.CreateUserRequest;
import com.hms.auth.dto.UpdateUserRequest;
import com.hms.auth.dto.UserResponse;
import com.hms.auth.entity.Role;
import com.hms.auth.entity.User;
import com.hms.auth.repository.RoleRepository;
import com.hms.auth.repository.UserRepository;
import com.hms.auth.util.UserMapper;
import com.hms.common.api.PageResponse;
import com.hms.common.enums.RoleType;
import com.hms.common.exception.BusinessException;
import com.hms.common.exception.DuplicateResourceException;
import com.hms.common.exception.ForbiddenException;
import com.hms.common.exception.ResourceNotFoundException;
import com.hms.common.security.AuthenticatedUser;
import com.hms.common.security.SecurityUtils;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserService {

	private final PasswordEncoder passwordEncoder;
	private final UserRepository userRepository;
	private final RoleRepository roleRepository;
	private final AuthService authService;

	@Transactional
	public UserResponse create(CreateUserRequest req) {
		if (userRepository.existsByUsernameIgnoreCase(req.username())) {
			throw new DuplicateResourceException("Username already taken");
		}
		if (userRepository.existsByEmailIgnoreCase(req.email())) {
			throw new DuplicateResourceException("Email already register");
		}
		Set<Role> roles = resolveRoles(req.roles());

		User user = new User();
		user.setUsername(req.username().trim());
		user.setEmail(req.email().trim().toLowerCase());
		user.setPhone(req.phone());
		user.setFirstName(req.firstName());
		user.setLastName(req.lastName());
		user.setPasswordHash(passwordEncoder.encode(req.password()));
		user.setMustChangePassword(true);
		user.setRoles(roles);
		user.setClinicId(req.clinicId() != null ? req.clinicId()
				: SecurityUtils.currentUser().map(AuthenticatedUser::clinicId).orElse(null));
		return UserMapper.toResponse(userRepository.save(user));
	}

	@Transactional(readOnly = true)
	public UserResponse get(UUID userId) {
		return UserMapper.toResponse(find(userId));
	}

	@Transactional(readOnly = true)
	public PageResponse<UserResponse> list(String search, Pageable pageable) {
		Page<User> page = (search == null || search.isBlank()) ? userRepository.findAll(pageable)
				: userRepository
						.findByUsernameContainingIgnoreCaseOrEmailContainingIgnoreCaseOrFirstNameContainingIgnoreCase(
								search, search, search, pageable);
		return PageResponse.from(page, UserMapper::toResponse);
	}

	@Transactional
	public UserResponse update(UUID id, UpdateUserRequest req) {
		User user = find(id);
		if (req.email() != null && !req.email().equalsIgnoreCase(user.getEmail())) {
			if (userRepository.existsByEmailIgnoreCase(req.email())) {
				throw new DuplicateResourceException("Email already registered");
			}
		}
		if (req.phone() != null)
			user.setPhone(req.phone());
		if (req.firstName() != null)
			user.setFirstName(req.firstName());
		if (req.lastName() != null)
			user.setLastName(req.lastName());
		if (req.enabled() != null) {
			user.setEnabled(req.enabled());
			if (!req.enabled())
				authService.logoutAll(id);
		}
		return UserMapper.toResponse(user);
	}

	@Transactional
	public UserResponse assignRoles(UUID id, Set<RoleType> roleTypes) {
		User user = find(id);
		user.setRoles(resolveRoles(roleTypes));
		authService.logoutAll(id); // permissions changed: force new token
		return UserMapper.toResponse(user);
	}

	/** Soft delete. Username/email are suffixed so they can be reused later. */
	@Transactional
	public void delete(UUID id) {
		boolean self = SecurityUtils.currentUser().map(u -> u.userId().equals(id)).orElse(false);

		if (self) {
			throw new BusinessException("You cannot delete your own account");
		}
		User user = find(id);
		authService.logoutAll(id);
		String suffix = "#del#" + id.toString().substring(0, 8);
		user.setUsername(user.getUsername() + suffix);
		user.setEmail(user.getEmail() + suffix);
		user.setEnabled(false);
		user.softDelete(SecurityUtils.currentUsername());
	}

	@Transactional
	public void forceLogout(UUID id) {
		find(id);
		authService.logoutAll(id);
	}

	// Helper methods
	private User find(UUID id) {
		return userRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("User", id));
	}

	private Set<Role> resolveRoles(Set<RoleType> types) {
		boolean callerIsSuperAdmin = SecurityUtils.currentUser()
				.map(u -> u.roles().contains(RoleType.SUPER_ADMIN.name())).orElse(false);

		if (types.contains(RoleType.SUPER_ADMIN) && !callerIsSuperAdmin) {
			throw new ForbiddenException("Only a SUPER_ADMIN can assign the SUPER_ADMIN role");
		}

		List<Role> found = roleRepository.findByNameIn(types);
		if (found.size() != types.size()) {
			throw new BusinessException("One or more roles do not exist");
		}

		return new HashSet<>(found);
	}
}
