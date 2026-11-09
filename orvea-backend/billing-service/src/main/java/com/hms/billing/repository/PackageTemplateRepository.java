package com.hms.billing.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.hms.billing.entity.PackageTemplate;

public interface PackageTemplateRepository extends JpaRepository<PackageTemplate, UUID> {
	List<PackageTemplate> findByClinicIdOrderByNameAsc(UUID clinicId);
}