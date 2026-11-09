package com.hms.measurement.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.hms.measurement.entity.MeasurementPlan;
import com.hms.measurement.enums.LimbType;
import com.hms.measurement.enums.MeasurementKind;
import com.hms.measurement.enums.PlanStatus;

public interface PlanRepository extends JpaRepository<MeasurementPlan, UUID> {

	boolean existsByCaseIdAndKindAndLimbTypeAndStatusNot(UUID caseId, MeasurementKind kind, LimbType limbType,
			PlanStatus status);

	@Query("""
			select p from MeasurementPlan p
			where (:clinicId is null or p.clinicId = :clinicId)
			  and (:patientId is null or p.patientId = :patientId)
			  and (:caseId is null or p.caseId = :caseId)
			  and (:status is null or p.status = :status)
			""")
	Page<MeasurementPlan> search(@Param("patientId") UUID patientId, @Param("caseId") UUID caseId,
			@Param("status") PlanStatus status, @Param("clinicId") UUID clinicId, Pageable pageable);

	@Query("""
			select p from MeasurementPlan p
			where p.status = :status and p.nextDueDate <= :until
			  and (:clinicId is null or p.clinicId = :clinicId)
			  and (:therapistId is null or p.therapistId = :therapistId)
			order by p.nextDueDate
			""")
	List<MeasurementPlan> findDue(@Param("status") PlanStatus status, @Param("clinicId") UUID clinicId,
			@Param("therapistId") UUID therapistId, @Param("until") LocalDate until);

	@Query("""
			select p.id from MeasurementPlan p
			where p.remindPatient = true and p.status = :status and p.nextDueDate between :from and :to
			""")
	List<UUID> findReminderCandidateIds(@Param("status") PlanStatus status, @Param("from") LocalDate from,
			@Param("to") LocalDate to);
}