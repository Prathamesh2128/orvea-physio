package com.hms.audit.repository;

import java.time.Instant;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.hms.audit.entity.AuditLog;

public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {

	boolean existsByEventId(String eventId);

	@Query("""
			select a from AuditLog a
			where (:clinicId is null or a.clinicId = :clinicId)
			  and (:entityType is null or a.entityType = :entityType)
			  and (:entityId is null or a.entityId = :entityId)
			  and (:actor is null or a.actor = :actor)
			  and (:action is null or a.action = :action)
			  and (:source is null or a.sourceService = :source)
			  and (:from is null or a.occurredAt >= :from)
			  and (:to is null or a.occurredAt < :to)
			""")
	Page<AuditLog> search(@Param("clinicId") UUID clinicId, @Param("entityType") String entityType,
			@Param("entityId") String entityId, @Param("actor") String actor, @Param("action") String action,
			@Param("source") String source, @Param("from") Instant from, @Param("to") Instant to, Pageable pageable);
}