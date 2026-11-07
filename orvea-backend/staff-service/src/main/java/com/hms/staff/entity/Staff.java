package com.hms.staff.entity;

import java.time.LocalDate;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.type.SqlTypes;

import com.hms.common.entity.BaseEntity;
import com.hms.common.enums.Gender;
import com.hms.common.enums.RoleType;
import com.hms.staff.enums.StaffStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@SuppressWarnings("serial")
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "staff", indexes = { @Index(name = "idx_staff_type_status", columnList = "staff_type,status"),
		@Index(name = "idx_staff_user", columnList = "user_id") })
@SQLRestriction("is_deleted = false")
public class Staff extends BaseEntity {
	/** Optional link to the auth-service login. */
	@JdbcTypeCode(SqlTypes.CHAR)
	@Column(name = "user_id", columnDefinition = "CHAR(36)")
	private UUID userId;

	@Column(name = "first_name", nullable = false, length = 60)
	private String firstName;

	@Column(name = "middle_name", length = 60)
	private String middleName;

	@Column(name = "last_name", nullable = false, length = 60)
	private String lastName;

	/** Computed on save, e.g. "Dr. Siddhi Prathamesh Shinde(PT)". */
	@Column(name = "display_name", nullable = false, length = 200)
	private String displayName;

	@Enumerated(EnumType.STRING)
	@Column(length = 10)
	private Gender gender;

	@Enumerated(EnumType.STRING)
	@Column(name = "staff_type", nullable = false, length = 30)
	private RoleType staffType;

	@Column(length = 20)
	private String phone;
	@Column(length = 150)
	private String email;
	@Column(length = 150)
	private String qualification;
	@Column(name = "registration_number", length = 60)
	private String registrationNumber;
	@Column(length = 150)
	private String specialization;
	@Column(name = "experience_years")
	private Integer experienceYears;
	@Column(name = "joining_date")
	private LocalDate joiningDate;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 10)
	private StaffStatus status = StaffStatus.ACTIVE;
}
