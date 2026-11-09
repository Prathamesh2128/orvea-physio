package com.hms.assessment.entity;

import java.time.LocalDateTime;

import org.hibernate.annotations.SQLRestriction;

import com.hms.assessment.enums.TemplateStatus;
import com.hms.common.entity.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * One form per condition code (or COMMON). clinic_id NULL = shared default,
 * read-only for clinics. Several versions can exist; at most one is PUBLISHED.
 */
@SuppressWarnings("serial")
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "assessment_templates", uniqueConstraints = @UniqueConstraint(name = "uk_template_clinic_code_version", columnNames = {
		"clinic_id", "code",
		"template_version" }), indexes = @Index(name = "idx_template_code_status", columnList = "code,status"))
@SQLRestriction("is_deleted = false")
public class AssessmentTemplate extends BaseEntity {

	/** COMMON, or a condition code from patient-service (LYMPHEDEMA, KNEE ...). */
	@Column(nullable = false, length = 60)
	private String code;

	@Column(nullable = false, length = 120)
	private String name;

	@Column(length = 500)
	private String description;

	/**
	 * Template revision (1, 2, 3...). Not BaseEntity.version, which is the
	 * optimistic-lock counter.
	 */
	@Column(name = "template_version", nullable = false)
	private int templateVersion = 1;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 12)
	private TemplateStatus status = TemplateStatus.DRAFT;

	/** TemplateDefinition as JSON. */
	@Column(columnDefinition = "JSON", nullable = false)
	private String definition;

	@Column(name = "published_at")
	private LocalDateTime publishedAt;
}