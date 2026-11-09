package com.hms.billing.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.hms.billing.entity.PackageUsage;

public interface PackageUsageRepository extends JpaRepository<PackageUsage, UUID> {
	List<PackageUsage> findByPackageIdOrderByCreatedAtDesc(UUID packageId);

	Optional<PackageUsage> findByIdAndPackageId(UUID id, UUID packageId);

	boolean existsByPackageIdAndReversedFalse(UUID packageId);

	boolean existsByAppointmentIdAndReversedFalse(UUID appointmentId);
}