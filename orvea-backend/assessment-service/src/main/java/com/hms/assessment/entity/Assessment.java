package com.hms.assessment.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.type.SqlTypes;

import com.hms.assessment.enums.AssessmentStatus;
import com.hms.assessment.enums.AssessmentType;
import com.hms.common.entity.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@SuppressWarnings("serial")
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "assessments", indexes = { @Index(name = "idx_assess_patient", columnList = "patient_id,visit_date"),
		@Index(name = "idx_assess_case", columnList = "case_id,status") })
@SQLRestriction("is_deleted = false")
public class Assessment extends BaseEntity {

	@JdbcTypeCode(SqlTypes.CHAR)
	@Column(name = "patient_id", columnDefinition = "CHAR(36)", nullable = false)
	private UUID patientId;

	@Column(name = "patient_uhid", length = 30)
	private String patientUhid;

	@JdbcTypeCode(SqlTypes.CHAR)
	@Column(name = "case_id", columnDefinition = "CHAR(36)", nullable = false)
	private UUID caseId;

	@Column(name = "case_number", length = 50)
	private String caseNumber;

	@JdbcTypeCode(SqlTypes.CHAR)
	@Column(name = "appointment_id", columnDefinition = "CHAR(36)")
	private UUID appointmentId;

	@Enumerated(EnumType.STRING)
	@Column(name = "assessment_type", nullable = false, length = 12)
	private AssessmentType assessmentType;

	@Column(name = "visit_date", nullable = false)
	private LocalDate visitDate;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 12)
	private AssessmentStatus status = AssessmentStatus.DRAFT;

	/**
	 * Frozen copy of the forms used: {"templates":[{code,name,version,sections}]}
	 */
	@Column(name = "template_snapshot", columnDefinition = "JSON", nullable = false)
	private String templateSnapshot;

	/** {"TEMPLATE_CODE": {"question_key": value}} */
	@Column(columnDefinition = "JSON", nullable = false)
	private String answers = "{}";

	/**
	 * "TEMPLATE.question" keys copied from the last visit and not yet confirmed by
	 * the clinician.
	 */
	@Column(name = "carried_forward", columnDefinition = "JSON", nullable = false)
	private String carriedForward = "[]";

	@JdbcTypeCode(SqlTypes.CHAR)
	@Column(name = "copied_from_id", columnDefinition = "CHAR(36)")
	private UUID copiedFromId;

	@Column(name = "completed_at")
	private LocalDateTime completedAt;

	@Column(name = "completed_by", length = 100)
	private String completedBy;
}