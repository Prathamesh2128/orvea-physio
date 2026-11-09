package com.hms.measurement.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.type.SqlTypes;

import com.hms.common.entity.BaseEntity;
import com.hms.measurement.enums.DueMode;
import com.hms.measurement.enums.Landmark;
import com.hms.measurement.enums.LimbType;
import com.hms.measurement.enums.MeasurementKind;
import com.hms.measurement.enums.PlanStatus;
import com.hms.measurement.enums.Side;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * What to measure on one case, how often, and when it is next due. Holds its
 * own copy of the protocol.
 */
@SuppressWarnings("serial")
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "measurement_plans", indexes = { @Index(name = "idx_plan_due", columnList = "status,next_due_date"),
		@Index(name = "idx_plan_therapist", columnList = "therapist_id,status,next_due_date"),
		@Index(name = "idx_plan_case", columnList = "case_id") })
@SQLRestriction("is_deleted = false")
public class MeasurementPlan extends BaseEntity {

	@JdbcTypeCode(SqlTypes.CHAR)
	@Column(name = "patient_id", columnDefinition = "CHAR(36)", nullable = false)
	private UUID patientId;
	@Column(name = "patient_uhid", length = 30)
	private String patientUhid;
	@Column(name = "patient_name", length = 150)
	private String patientName;

	@JdbcTypeCode(SqlTypes.CHAR)
	@Column(name = "case_id", columnDefinition = "CHAR(36)", nullable = false)
	private UUID caseId;
	@Column(name = "case_number", length = 50)
	private String caseNumber;

	/** Staff member responsible (drives the therapist's due list). */
	@JdbcTypeCode(SqlTypes.CHAR)
	@Column(name = "therapist_id", columnDefinition = "CHAR(36)")
	private UUID therapistId;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 10)
	private MeasurementKind kind;

	// ---- protocol (limb only)
	@Enumerated(EnumType.STRING)
	@Column(name = "limb_type", length = 10)
	private LimbType limbType;
	@Column(name = "step_cm", precision = 5, scale = 2)
	private BigDecimal stepCm;
	@Enumerated(EnumType.STRING)
	@Column(name = "start_landmark", length = 20)
	private Landmark startLandmark;
	@Column(name = "point_count")
	private Integer pointCount;

	/**
	 * Bumped when the protocol is changed after readings exist. Not
	 * BaseEntity.version (lock counter).
	 */
	@Column(name = "protocol_revision", nullable = false)
	private int protocolRevision = 1;

	/** Reference side for the difference percentage. */
	@Enumerated(EnumType.STRING)
	@Column(name = "affected_side", length = 10)
	private Side affectedSide;

	// ---- schedule
	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 10)
	private PlanStatus status = PlanStatus.ACTIVE;
	@Column(name = "repeat_days", nullable = false)
	private int repeatDays = 5;
	@Enumerated(EnumType.STRING)
	@Column(name = "due_mode", nullable = false, length = 15)
	private DueMode dueMode = DueMode.FROM_ACTUAL;
	@Column(name = "next_due_date")
	private LocalDate nextDueDate;
	@Column(name = "last_measured_on")
	private LocalDate lastMeasuredOn;
	@Column(name = "started_on", nullable = false)
	private LocalDate startedOn;

	@Column(name = "remind_patient", nullable = false)
	private boolean remindPatient = false;
	/**
	 * The due date a patient reminder was already queued for (prevents repeats).
	 */
	@Column(name = "last_reminder_due_date")
	private LocalDate lastReminderDueDate;

	@Column(length = 500)
	private String note;
}