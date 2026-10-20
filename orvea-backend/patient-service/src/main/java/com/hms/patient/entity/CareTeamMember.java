package com.hms.patient.entity;

import org.hibernate.annotations.SQLRestriction;

import com.hms.common.entity.BaseEntity;
import com.hms.patient.enums.CareTeamRole;

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
@Table(name = "care_team_members", indexes = @Index(name = "idx_care_patient", columnList = "patient_id"))
@SQLRestriction("is_deleted = false")
public class CareTeamMember extends BaseEntity{
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "patient_id", nullable = false)
	private Patient patient;

	// Optional: member belongs to one specific case (e.g. the surgeon for this surgery).
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "case_id")
	private TreatmentCase treatmentCase;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 25)
	private CareTeamRole role;

	@Column(nullable = false, length = 120)
	private String name;

	@Column(length = 20)
	private String phone;

	@Column(length = 150)
	private String email;

	@Column(length = 150)
	private String organization;

	@Column(length = 60)
	private String relationship;

	@Column(name = "primary_contact", nullable = false)
	private boolean primaryContact = false;

	@Column(length = 500)
	private String notes;
}
