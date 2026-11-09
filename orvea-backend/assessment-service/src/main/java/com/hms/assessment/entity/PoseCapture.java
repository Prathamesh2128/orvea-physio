package com.hms.assessment.entity;

import java.time.LocalDateTime;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.type.SqlTypes;

import com.hms.assessment.enums.CaptureKind;
import com.hms.assessment.enums.CaptureStatus;
import com.hms.assessment.enums.CaptureView;
import com.hms.common.entity.BaseEntity;
import com.hms.common.enums.BodyRegion;
import com.hms.common.enums.Laterality;

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
 * Assistive camera measurement. The browser (e.g. MediaPipe) computes the
 * angles; the server keeps only angles, never landmark coordinates. A capture
 * is a SUGGESTION until a clinician confirms it.
 */
@SuppressWarnings("serial")
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "pose_captures", indexes = @Index(name = "idx_capture_assessment", columnList = "assessment_id,status"))
@SQLRestriction("is_deleted = false")
public class PoseCapture extends BaseEntity {

	@JdbcTypeCode(SqlTypes.CHAR)
	@Column(name = "assessment_id", columnDefinition = "CHAR(36)", nullable = false)
	private UUID assessmentId;

	@JdbcTypeCode(SqlTypes.CHAR)
	@Column(name = "patient_id", columnDefinition = "CHAR(36)", nullable = false)
	private UUID patientId;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 10)
	private CaptureKind kind;

	@Enumerated(EnumType.STRING)
	@Column(name = "view_type", length = 10)
	private CaptureView viewType;

	@Enumerated(EnumType.STRING)
	@Column(name = "body_region", length = 20)
	private BodyRegion bodyRegion;

	@Enumerated(EnumType.STRING)
	@Column(length = 20)
	private Laterality laterality;

	/** {"knee_flexion": 112.4, ...} as measured by the app. */
	@Column(columnDefinition = "JSON", nullable = false)
	private String angles;

	/**
	 * Final values after clinician confirmation (may differ from the suggestion).
	 */
	@Column(name = "confirmed_angles", columnDefinition = "JSON")
	private String confirmedAngles;

	/** 0..1 pose-detection confidence reported by the app. */
	@Column(name = "quality_score")
	private Double qualityScore;

	@Column(name = "tool_name", length = 60)
	private String toolName;

	@Column(name = "tool_version", length = 40)
	private String toolVersion;

	/** "TEMPLATE_CODE.question_key" this capture is meant to fill. */
	@Column(name = "question_ref", length = 120)
	private String questionRef;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 10)
	private CaptureStatus status = CaptureStatus.SUGGESTED;

	@Column(length = 500)
	private String note;

	/** Optional photo. Relative path in the image store. */
	@Column(name = "image_ref", length = 200)
	private String imageRef;

	@Column(name = "image_content_type", length = 30)
	private String imageContentType;

	@Column(name = "image_size_bytes")
	private Long imageSizeBytes;

	@Column(name = "confirmed_by", length = 100)
	private String confirmedBy;

	@Column(name = "confirmed_at")
	private LocalDateTime confirmedAt;
}