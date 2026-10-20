package com.hms.patient.entity;

import java.time.LocalDate;

import org.hibernate.annotations.SQLRestriction;

import com.hms.common.entity.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(name = "case_conditions", indexes = @Index(name = "idx_cond_case", columnList = "case_id"))
@SQLRestriction("is_deleted = false")
public class CaseCondition extends BaseEntity {
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "case_id", nullable = false)
	private TreatmentCase treatmentCase;

	/** Code from condition_master; assessment templates key off this. */
	@Column(name = "condition_code", nullable = false, length = 50)
	private String conditionCode;

	/** Name snapshot so history stays readable if the master is renamed. */
	@Column(name = "condition_name", nullable = false, length = 120)
	private String conditionName;

	@Column(length = 500)
	private String notes;

	@Column(name = "diagnosed_on")
	private LocalDate diagnosedOn;

	@Column(nullable = false)
	private boolean active = true;
}
