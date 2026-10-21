package com.hms.patient.entity;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.type.SqlTypes;

import com.hms.common.entity.BaseEntity;
import com.hms.common.enums.BodyRegion;
import com.hms.common.enums.Laterality;
import com.hms.patient.enums.CaseStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@SuppressWarnings("serial")
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "treatment_cases", indexes = { @Index(name = "idx_case_patient", columnList = "patient_id"),
		@Index(name = "idx_case_therapist_status", columnList = "primary_therapist_id,status") })
@SQLRestriction("is_deleted = false")
public class TreatmentCase extends BaseEntity {
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "patient_id", nullable = false)
	private Patient patient;

	@Column(name = "case_number", nullable = false, length = 50)
	private String caseNumber;

	@Column(nullable = false, length = 255)
	private String diagnosis;

	@Column(name = "icd_code", length = 20)
	private String icdCode;

	@Enumerated(EnumType.STRING)
	@Column(name = "body_region", nullable = false, length = 20)
	private BodyRegion bodyRegion;

	@Enumerated(EnumType.STRING)
	@Column(length = 20)
	private Laterality laterality;

	/** Reference to staff-service (no cross-database FK). */
	@JdbcTypeCode(SqlTypes.CHAR)
	@Column(name = "primary_therapist_id", columnDefinition = "CHAR(36)")
	private UUID primaryTherapistId;

	@Column(name = "planned_sessions", nullable = false)
	private int plannedSessions;

	@Column(name = "completed_sessions", nullable = false)
	private int completedSessions = 0;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 15)
	private CaseStatus status = CaseStatus.PLANNED;

	@Column(name = "start_date")
	private LocalDate startDate;

	@Column(name = "expected_end_date")
	private LocalDate expectedEndDate;

	@Column(name = "closed_on")
	private LocalDate closedOn;

	@Column(name = "closure_reason", length = 255)
	private String closureReason;

	@Column(length = 1000)
	private String notes;

	private List<CaseCondition> conditions = new ArrayList<>();
}
