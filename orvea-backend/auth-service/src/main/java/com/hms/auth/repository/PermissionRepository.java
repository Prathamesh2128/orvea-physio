package com.hms.auth.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.hms.auth.entity.Permission;

public interface PermissionRepository extends JpaRepository<Permission, UUID> {

}
