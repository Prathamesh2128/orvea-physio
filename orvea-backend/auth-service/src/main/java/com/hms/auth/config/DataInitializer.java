package com.hms.auth.config;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.hms.auth.entity.Permission;
import com.hms.auth.entity.Role;
import com.hms.auth.entity.User;
import com.hms.auth.repository.PermissionRepository;
import com.hms.auth.repository.RoleRepository;
import com.hms.auth.repository.UserRepository;
import com.hms.common.enums.PermissionType;
import com.hms.common.enums.RoleType;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements ApplicationRunner {
	private final PermissionRepository permissionRepository;
	private final RoleRepository roleRepository;
	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;

	@Value("${physio.bootstrap.admin.username}")
	private String adminUsername;
	
	@Value("${physio.bootstrap.admin.email}")
	private String adminEmail;
	
	@Value("${physio.bootstrap.admin.password}")
	private String adminPassword;

	@Override
	@Transactional
	public void run(ApplicationArguments args) {
		for (PermissionType type : PermissionType.values()) {
			if (permissionRepository.findByName(type).isEmpty()) {
				Permission p = new Permission();
				p.setName(type);
				p.setModule(type.name().split("_")[0]);
				p.setDescription(type.name().replace('_', ' ').toLowerCase());
				permissionRepository.save(p);
			}
		}

		for (RoleType type : RoleType.values()) {
			Role role = roleRepository.findByName(type).orElseGet(() -> {
				Role r = new Role();
				r.setName(type);
				r.setDescription(type.name().replace('_', ' ').toLowerCase());
				return r;
			});
			Set<PermissionType> wanted = RolePermissionMatrix.forRole(type);
			Set<Permission> perms = wanted.stream().map(w -> permissionRepository.findByName(w).orElseThrow())
					.collect(Collectors.toCollection(HashSet::new));
			role.setPermissions(perms);
			roleRepository.save(role);
		}

		if (!userRepository.existsByUsernameIgnoreCase(adminUsername)) {
			User admin = new User();
			admin.setUsername(adminUsername);
			admin.setEmail(adminEmail);
			admin.setFirstName("Super");
			admin.setLastName("Admin");
			admin.setPasswordHash(passwordEncoder.encode(adminPassword));
			admin.setMustChangePassword(true);
			admin.setRoles(new HashSet<>(Set.of(roleRepository.findByName(RoleType.SUPER_ADMIN).orElseThrow())));
			userRepository.save(admin);
			log.warn("Bootstrap SUPER_ADMIN '{}' created. Change the password immediately.", adminUsername);
		}
	}
}
