package com.hms.measurement.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.type.SqlTypes;

import com.hms.common.entity.BaseEntity;
import com.hms.measurement.enums.MeasurementKind;
import com.hms.measurement.enums.RecordStatus;
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
 * One measurement session. Never edited: a wrong one is voided and re-taken.
 */
@SuppressWarnings("serial")
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "measurement_records", indexes = @Index(name = "idx_record_plan_date", columnList = "plan_id,status,measured_on"))
@SQLRestriction("is_deleted = false")
public class MeasurementRecord extends BaseEntity {

	@JdbcTypeCode(SqlTypes.CHAR)
	@Column(name = "plan_id", columnDefinition = "CHAR(36)", nullable = false)
	private UUID planId;
	@JdbcTypeCode(SqlTypes.CHAR)
	@Column(name = "patient_id", columnDefinition = "CHAR(36)", nullable = false)
	private UUID patientId;
	@JdbcTypeCode(SqlTypes.CHAR)
	@Column(name = "case_id", columnDefinition = "CHAR(36)", nullable = false)
	private UUID caseId;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 10)
	private MeasurementKind kind;

	@Column(name = "measured_on", nullable = false)
	private LocalDate measuredOn;
	@Column(name = "measured_time")
	private LocalTime measuredTime;

	@Column(name = "protocol_revision", nullable = false)
	private int protocolRevision;

	/** ProtocolSnapshot as JSON. */
	@Column(name = "protocol_snapshot", columnDefinition = "JSON", nullable = false)
	private String protocolSnapshot;

	/** Results as JSON: every reading plus computed differences. */
	@Column(columnDefinition = "JSON", nullable = false)
	private String results;

	/** The reference side actually used for this record. */
	@Enumerated(EnumType.STRING)
	@Column(name = "affected_side", length = 10)
	private Side affectedSide;

	// summary columns (for trend and charts)
	@Column(name = "mean_diff_cm", precision = 8, scale = 2)
	private BigDecimal meanDiffCm;
	@Column(name = "max_diff_cm", precision = 8, scale = 2)
	private BigDecimal maxDiffCm;
	@Column(name = "mean_diff_percent", precision = 8, scale = 2)
	private BigDecimal meanDiffPercent;
	@Column(name = "max_diff_percent", precision = 8, scale = 2)
	private BigDecimal maxDiffPercent;

	@Column(nullable = false)
	private boolean flagged = false;

	@Column(length = 500)
	private String notes;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 10)
	private RecordStatus status = RecordStatus.ACTIVE;
	@Column(name = "void_reason", length = 255)
	private String voidReason;

	@Column(name = "measured_by", length = 100)
	private String measuredBy;

	// optional photo
	@Column(name = "photo_ref", length = 200)
	private String photoRef;
	@Column(name = "photo_content_type", length = 30)
	private String photoContentType;
	@Column(name = "photo_size_bytes")
	private Long photoSizeBytes;
}