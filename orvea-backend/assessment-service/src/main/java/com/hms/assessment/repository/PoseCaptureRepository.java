package com.hms.assessment.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.hms.assessment.entity.PoseCapture;
import com.hms.assessment.enums.CaptureStatus;

public interface PoseCaptureRepository extends JpaRepository<PoseCapture, UUID> {
	List<PoseCapture> findByAssessmentIdOrderByCreatedAtAsc(UUID assessmentId);

	Optional<PoseCapture> findByIdAndAssessmentId(UUID id, UUID assessmentId);

	long countByAssessmentIdAndStatus(UUID assessmentId, CaptureStatus status);
}