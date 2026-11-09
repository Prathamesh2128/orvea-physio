package com.hms.measurement.repository;

import com.hms.measurement.entity.PlanEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PlanEventRepository extends JpaRepository<PlanEvent, UUID> {
	List<PlanEvent> findByPlanIdOrderByCreatedAtDesc(UUID planId);
}