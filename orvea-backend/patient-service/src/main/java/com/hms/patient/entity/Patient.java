package com.hms.patient.entity;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.type.SqlTypes;

import com.hms.common.entity.BaseEntity;
import com.hms.common.enums.Gender;
import com.hms.patient.enums.PatientStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.OneToMany;
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
@Table(name = "patients", uniqueConstraints = @UniqueConstraint(name = "uk_patient_clinic_uhid", columnNames = {
		"clinic_id", "uhid" }), indexes = { @Index(name = "idx_patient_phone", columnList = "phone"),
				@Index(name = "idx_patient_name", columnList = "first_name,last_name"),
				@Index(name = "idx_patient_user", columnList = "user_id") })
@SQLRestriction("is_deleted = false")
public class Patient extends BaseEntity {
	@Column(nullable = false, length = 30)
	private String uhid;

	@Column(name = "first_name", nullable = false, length = 60)
	private String firstName;

	@Column(name = "last_name", length = 60)
	private String lastName;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 10)
	private Gender gender;

	@Column(name = "date_of_birth", nullable = false)
	private LocalDate dateOfBirth;

	@Column(name = "dob_approximate", nullable = false)
	private boolean dobApproximate = false;

	@Column(name = "blood_group", length = 5)
	private String bloodGroup;

	@Column(name = "country_code", length = 5)
	private String countryCode = "+91";

	@Column(nullable = false, length = 20)
	private String phone;

	@Column(name = "alternate_phone", length = 20)
	private String alternatePhone;

	@Column(length = 150)
	private String email;

	@Column(name = "address_line", length = 255)
	private String addressLine;

	@Column(length = 80)
	private String city;

	@Column(length = 80)
	private String state;

	@Column(length = 12)
	private String pincode;

	@Column(length = 100)
	private String occupation;

	@Column(name = "referred_by", length = 150)
	private String referredBy;

	@Column(name = "emergency_contact_name", length = 100)
	private String emergencyContactName;

	@Column(name = "emergency_contact_phone", length = 20)
	private String emergencyContactPhone;

	@Column(name = "emergency_contact_relation", length = 50)
	private String emergencyContactRelation;

	/** Optional link to auth-service user (patient portal login). */
	@JdbcTypeCode(SqlTypes.CHAR)
	@Column(name = "user_id", columnDefinition = "CHAR(36)")
	private UUID userId;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 15)
	private PatientStatus status = PatientStatus.ACTIVE;

	@Column(name = "registered_on", nullable = false)
	private LocalDate registeredOn;

	// Monotonic counter used to build case numbers; never reused after soft delete.
	@Column(name = "case_counter", nullable = false)
	private int caseCounter = 0;

	@Column(length = 1000)
	private String notes;

	@OneToMany(mappedBy = "patient", fetch = FetchType.LAZY)
	private List<TreatmentCase> cases = new ArrayList<>();

	public String getFullName() {
		return lastName == null || lastName.isBlank() ? firstName : firstName + " " + lastName;
	}
}
