package com.hms.patient.entity;

import java.time.LocalDateTime;

import org.hibernate.annotations.SQLRestriction;

import com.hms.common.entity.BaseEntity;
import com.hms.patient.enums.ConsentGivenBy;
import com.hms.patient.enums.ConsentStatus;
import com.hms.patient.enums.ConsentType;

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
@Table(name = "patient_consents", indexes = @Index(name = "idx_consent_patient_type", columnList = "patient_id,consent_type,status"))
@SQLRestriction("is_deleted = false")
public class PatientConsent extends BaseEntity {

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "patient_id", nullable = false)
	private Patient patient;

	@Enumerated(EnumType.STRING)
	@Column(name = "consent_type", nullable = false, length = 30)
	private ConsentType consentType;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 15)
	private ConsentStatus status = ConsentStatus.GRANTED;

	@Enumerated(EnumType.STRING)
	@Column(name = "given_by", nullable = false, length = 15)
	private ConsentGivenBy givenBy;

	@Column(name = "signer_name", length = 120)
	private String signerName;

	@Column(name = "consent_version", length = 20)
	private String consentVersion;

	@Column(name = "document_ref", length = 255)
	private String documentRef;

	@Column(name = "granted_at", nullable = false)
	private LocalDateTime grantedAt;

	@Column(name = "withdrawn_at")
	private LocalDateTime withdrawnAt;

	@Column(name = "withdrawal_reason", length = 255)
	private String withdrawalReason;

	@Column(length = 500)
	private String notes;
}
