package com.hms.patient.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.hms.patient.entity.UhidCounter;

import jakarta.persistence.LockModeType;

public interface UhidCounterRepository extends JpaRepository<UhidCounter, String> {

	// Race-safe create: two first registrations cannot collide on the primary key.
	@Modifying
	@Query(value = "INSERT IGNORE INTO uhid_counters (id, last_number) VALUES (:id, 0)", nativeQuery = true)
	void initIfAbsent(@Param("id") String id);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("SELECT c FROM UhidCounter c WHERE c.id = :id")
	Optional<UhidCounter> findForUpdate(@Param("id") String id);
}
