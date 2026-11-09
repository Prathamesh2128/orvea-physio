package com.hms.measurement.repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.hms.measurement.entity.MeasurementRecord;
import com.hms.measurement.enums.RecordStatus;

public interface RecordRepository extends JpaRepository<MeasurementRecord, UUID> {

	long countByPlanIdAndStatus(UUID planId, RecordStatus status);

	Optional<MeasurementRecord> findByIdAndPlanId(UUID id, UUID planId);

	List<MeasurementRecord> findByPlanIdAndStatusOrderByMeasuredOnAscCreatedAtAsc(UUID planId, RecordStatus status);

	@Query("select r from MeasurementRecord r where r.planId = :planId and r.status = :status order by r.measuredOn desc, r.createdAt desc")
	List<MeasurementRecord> findRecent(@Param("planId") UUID planId, @Param("status") RecordStatus status,
			Pageable page);

	/** The reading just before this one, taken under the same protocol revision. */
	@Query("""
			select r from MeasurementRecord r
			where r.planId = :planId and r.status = :status and r.protocolRevision = :revision and r.id <> :excludeId
			  and (r.measuredOn < :date or (r.measuredOn = :date and r.createdAt < :createdAt))
			order by r.measuredOn desc, r.createdAt desc
			""")
	List<MeasurementRecord> findPrevious(@Param("planId") UUID planId, @Param("status") RecordStatus status,
			@Param("revision") int revision, @Param("excludeId") UUID excludeId, @Param("date") LocalDate date,
			@Param("createdAt") LocalDateTime createdAt, Pageable page);
}