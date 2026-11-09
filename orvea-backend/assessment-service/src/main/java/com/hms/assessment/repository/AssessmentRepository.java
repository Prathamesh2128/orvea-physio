package com.hms.assessment.repository;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.hms.assessment.entity.Assessment;
import com.hms.assessment.enums.AssessmentStatus;
import com.hms.assessment.enums.AssessmentType;

public interface AssessmentRepository extends JpaRepository<Assessment, UUID> {

	long countByCaseId(UUID caseId);

	boolean existsByCaseIdAndVisitDateAndAssessmentTypeAndStatus(UUID caseId, LocalDate visitDate, AssessmentType type,
			AssessmentStatus status);

	Optional<Assessment> findFirstByCaseIdAndStatusOrderByVisitDateDescCompletedAtDesc(UUID caseId,
			AssessmentStatus status);

	@Query("""
			select a from Assessment a
			where (:clinicId is null or a.clinicId = :clinicId)
			  and (:patientId is null or a.patientId = :patientId)
			  and (:caseId is null or a.caseId = :caseId)
			  and (:status is null or a.status = :status)
			""")
	Page<Assessment> search(@Param("patientId") UUID patientId, @Param("caseId") UUID caseId,
			@Param("status") AssessmentStatus status, @Param("clinicId") UUID clinicId, Pageable pageable);
}