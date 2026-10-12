package com.hms.auth.repository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.hms.auth.entity.Role;
import com.hms.common.enums.RoleType;

public interface RoleRepository extends JpaRepository<Role, UUID> {
	List<Role> findByNameIn(Collection<RoleType> name);
}
