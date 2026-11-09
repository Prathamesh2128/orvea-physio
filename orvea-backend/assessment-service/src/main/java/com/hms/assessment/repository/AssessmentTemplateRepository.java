package com.hms.assessment.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.hms.assessment.entity.AssessmentTemplate;
import com.hms.assessment.enums.TemplateStatus;

public interface AssessmentTemplateRepository extends JpaRepository<AssessmentTemplate, UUID> {

	List<AssessmentTemplate> findByClinicIdAndCodeOrderByTemplateVersionDesc(UUID clinicId, String code);

	Optional<AssessmentTemplate> findFirstByClinicIdAndCodeAndStatus(UUID clinicId, String code, TemplateStatus status);

	/** The clinic's own templates plus the shared defaults. */
	@Query("select t from AssessmentTemplate t where t.clinicId = :clinicId or t.clinicId is null order by t.code, t.templateVersion desc")
	List<AssessmentTemplate> findVisibleTo(@Param("clinicId") UUID clinicId);
}