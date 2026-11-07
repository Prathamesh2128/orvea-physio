package com.hms.appointment.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.type.SqlTypes;

import com.hms.appointment.enums.AppointmentStatus;
import com.hms.appointment.enums.BookingSource;
import com.hms.common.entity.BaseEntity;

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
@Table(name = "appointments", indexes = {
		@Index(name = "idx_appt_provider_date", columnList = "provider_id,appointment_date,status"),
		@Index(name = "idx_appt_patient_date", columnList = "patient_id,appointment_date") })
@SQLRestriction("is_deleted = false")
public class Appointment extends BaseEntity {

	@JdbcTypeCode(SqlTypes.CHAR)
	@Column(name = "patient_id", columnDefinition = "CHAR(36)", nullable = false)
	private UUID patientId;
	@Column(name = "patient_uhid", length = 30)
	private String patientUhid;
	@Column(name = "patient_name", length = 150)
	private String patientName;
	@Column(name = "patient_phone", length = 20)
	private String patientPhone;

	@JdbcTypeCode(SqlTypes.CHAR)
	@Column(name = "provider_id", columnDefinition = "CHAR(36)", nullable = false)
	private UUID providerId;
	@Column(name = "provider_name", length = 200)
	private String providerName;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "visit_type_id", nullable = false)
	private VisitType visitType;

	/** Optional link to a treatment case in patient-service. */
	@JdbcTypeCode(SqlTypes.CHAR)
	@Column(name = "case_id", columnDefinition = "CHAR(36)")
	private UUID caseId;

	@Column(name = "appointment_date", nullable = false)
	private LocalDate appointmentDate;

	@Column(name = "start_time", nullable = false)
	private LocalTime startTime;

	@Column(name = "end_time", nullable = false)
	private LocalTime endTime;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 15)
	private AppointmentStatus status = AppointmentStatus.BOOKED;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 10)
	private BookingSource source = BookingSource.PHONE;

	@Column(length = 500)
	private String note;

	@Column(name = "cancellation_reason", length = 255)
	private String cancellationReason;

	@Column(name = "cancelled_at")
	private LocalDateTime cancelledAt;

	@JdbcTypeCode(SqlTypes.CHAR)
	@Column(name = "rescheduled_from_id", columnDefinition = "CHAR(36)")
	private UUID rescheduledFromId;
}
