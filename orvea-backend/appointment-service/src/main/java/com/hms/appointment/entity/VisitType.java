package com.hms.appointment.entity;

import org.hibernate.annotations.SQLRestriction;

import com.hms.common.entity.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@SuppressWarnings("serial")
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "visit_types", uniqueConstraints = @UniqueConstraint(name = "uk_visit_type_clinic_code", columnNames = {
		"clinic_id", "code" }))
@SQLRestriction("is_deleted = false")
public class VisitType extends BaseEntity {

	@Column(nullable = false, length = 40)
	private String code;

	@Column(nullable = false, length = 100)
	private String name;

	@Column(name = "duration_minutes", nullable = false)
	private int durationMinutes;

	/**
	 * When true, a completed visit should count towards the case's planned sessions
	 * (wired in Phase 5).
	 */
	@Column(name = "counts_as_session", nullable = false)
	private boolean countsAsSession = false;

	@Column(nullable = false)
	private boolean active = true;

	@Column(name = "sort_order", nullable = false)
	private int sortOrder = 0;
}
