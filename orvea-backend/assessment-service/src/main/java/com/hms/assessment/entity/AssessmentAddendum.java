package com.hms.assessment.entity;

import com.hms.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.type.SqlTypes;

import java.util.UUID;

/** Append-only correction note on a completed assessment. */
@SuppressWarnings("serial")
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "assessment_addenda", indexes = @Index(name = "idx_addendum_assessment", columnList = "assessment_id"))
@SQLRestriction("is_deleted = false")
public class AssessmentAddendum extends BaseEntity {

	@JdbcTypeCode(SqlTypes.CHAR)
	@Column(name = "assessment_id", columnDefinition = "CHAR(36)", nullable = false)
	private UUID assessmentId;

	@Column(nullable = false, length = 2000)
	private String text;

	@Column(nullable = false, length = 100)
	private String author;
}