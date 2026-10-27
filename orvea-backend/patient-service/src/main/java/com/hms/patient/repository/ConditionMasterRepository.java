package com.hms.patient.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.hms.patient.entity.ConditionMaster;

public interface ConditionMasterRepository extends JpaRepository<ConditionMaster, UUID> {
	Optional<ConditionMaster> findByCodeIgnoreCase(String code);

	boolean existsByCodeIgnoreCase(String code);

	List<ConditionMaster> findByActiveTrueOrderBySortOrderAscNameAsc();

	List<ConditionMaster> findAllByOrderBySortOrderAscNameAsc();
}
