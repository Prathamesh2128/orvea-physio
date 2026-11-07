package com.hms.appointment.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.hms.appointment.entity.SlotLock;

import jakarta.persistence.LockModeType;

public interface SlotLockRepository extends JpaRepository<SlotLock, String> {

	@Modifying
	@Query(value = "INSERT IGNORE INTO slot_locks (id) VALUES (:id)", nativeQuery = true)
	void initIfAbsent(@Param("id") String id);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select l from SlotLock l where l.id = :id")
	Optional<SlotLock> findForUpdate(@Param("id") String id);
}
