package com.hms.billing.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.hms.billing.entity.ServiceItem;

public interface ServiceItemRepository extends JpaRepository<ServiceItem, UUID> {
	Optional<ServiceItem> findByClinicIdAndCodeIgnoreCase(UUID clinicId, String code);

	List<ServiceItem> findByClinicIdOrderBySortOrderAscNameAsc(UUID clinicId);

	List<ServiceItem> findByClinicIdAndVisitTypeCodeIgnoreCaseAndActiveTrue(UUID clinicId, String visitTypeCode);
}