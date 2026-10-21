package com.hms.patient.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.hms.patient.entity.ConditionMaster;

public interface ConditionMasterRepository extends JpaRepository<ConditionMaster, UUID> {
	boolean existsByCodeIgnoreCase(String code);
}
