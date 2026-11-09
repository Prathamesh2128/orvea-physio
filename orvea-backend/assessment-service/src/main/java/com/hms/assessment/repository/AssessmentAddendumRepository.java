package com.hms.assessment.repository;

import com.hms.assessment.entity.AssessmentAddendum;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AssessmentAddendumRepository extends JpaRepository<AssessmentAddendum, UUID> {
	List<AssessmentAddendum> findByAssessmentIdOrderByCreatedAtAsc(UUID assessmentId);
}