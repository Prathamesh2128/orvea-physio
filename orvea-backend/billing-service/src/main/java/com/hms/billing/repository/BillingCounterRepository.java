package com.hms.billing.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.hms.billing.entity.BillingCounter;

import jakarta.persistence.LockModeType;

public interface BillingCounterRepository extends JpaRepository<BillingCounter, String> {

	@Modifying
	@Query(value = "INSERT IGNORE INTO billing_counters (id, last_number) VALUES (:id, 0)", nativeQuery = true)
	void initIfAbsent(@Param("id") String id);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select c from BillingCounter c where c.id = :id")
	Optional<BillingCounter> findForUpdate(@Param("id") String id);
}