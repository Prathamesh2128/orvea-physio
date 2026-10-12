package com.hms.auth.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.hms.auth.entity.Permission;
import com.hms.common.enums.PermissionType;

public interface PermissionRepository extends JpaRepository<Permission, UUID> {
	Optional<Permission> findByName(PermissionType name);
}
