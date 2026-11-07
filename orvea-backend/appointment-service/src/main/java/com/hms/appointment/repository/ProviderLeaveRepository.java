package com.hms.appointment.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.hms.appointment.entity.ProviderLeave;

public interface ProviderLeaveRepository extends JpaRepository<ProviderLeave, UUID> {

	@Query("select l from ProviderLeave l where l.providerId = :pid and l.fromDate <= :date and l.toDate >= :date")
	List<ProviderLeave> findCovering(@Param("pid") UUID providerId, @Param("date") LocalDate date);

	List<ProviderLeave> findByProviderIdAndToDateGreaterThanEqualOrderByFromDateAsc(UUID providerId, LocalDate from);

	Optional<ProviderLeave> findByIdAndProviderId(UUID id, UUID providerId);
}
