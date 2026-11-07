package com.hms.appointment.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.hms.appointment.entity.VisitType;

public interface VisitTypeRepository extends JpaRepository<VisitType, UUID> {
	long countByClinicId(UUID clinicId);

	Optional<VisitType> findByClinicIdAndCodeIgnoreCase(UUID clinicId, String code);

	List<VisitType> findByClinicIdOrderBySortOrderAscNameAsc(UUID clinicId);
}
