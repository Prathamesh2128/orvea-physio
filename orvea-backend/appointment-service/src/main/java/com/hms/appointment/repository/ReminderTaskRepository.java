package com.hms.appointment.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.hms.appointment.entity.ReminderTask;
import com.hms.appointment.enums.ReminderStatus;

public interface ReminderTaskRepository extends JpaRepository<ReminderTask, UUID> {

	List<ReminderTask> findByAppointmentIdOrderByRemindAtAsc(UUID appointmentId);

	@Query("select t.id from ReminderTask t where t.status = :status and t.remindAt <= :now order by t.remindAt")
	List<UUID> findDueIds(@Param("status") ReminderStatus status, @Param("now") LocalDateTime now, Pageable page);

	/**
	 * Atomic claim: only one node wins, so a reminder is never dispatched twice.
	 */
	@Modifying(clearAutomatically = true)
	@Query("""
			update ReminderTask t set t.status = :to, t.dispatchedAt = :now, t.attempts = t.attempts + 1
			where t.id = :id and t.status = :from
			""")
	int claim(@Param("id") UUID id, @Param("from") ReminderStatus from, @Param("to") ReminderStatus to,
			@Param("now") LocalDateTime now);

	@Modifying(clearAutomatically = true)
	@Query("update ReminderTask t set t.status = :to where t.appointmentId = :aid and t.status = :from")
	int cancelPending(@Param("aid") UUID appointmentId, @Param("from") ReminderStatus from,
			@Param("to") ReminderStatus to);
}
