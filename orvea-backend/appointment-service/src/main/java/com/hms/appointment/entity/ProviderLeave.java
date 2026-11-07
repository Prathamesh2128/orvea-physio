package com.hms.appointment.entity;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.type.SqlTypes;

import com.hms.appointment.enums.LeaveType;
import com.hms.common.entity.BaseEntity;

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
@Table(name = "provider_leaves", indexes = @Index(name = "idx_leave_provider_dates", columnList = "provider_id,from_date,to_date"))
@SQLRestriction("is_deleted = false")
public class ProviderLeave extends BaseEntity {

	@JdbcTypeCode(SqlTypes.CHAR)
	@Column(name = "provider_id", columnDefinition = "CHAR(36)", nullable = false)
	private UUID providerId;

	@Column(name = "from_date", nullable = false)
	private LocalDate fromDate;

	@Column(name = "to_date", nullable = false)
	private LocalDate toDate;

	@Column(name = "from_time")
	private LocalTime fromTime;

	@Column(name = "to_time")
	private LocalTime toTime;

	@Enumerated(EnumType.STRING)
	@Column(name = "leave_type", nullable = false, length = 15)
	private LeaveType leaveType = LeaveType.OTHER;

	@Column(length = 255)
	private String reason;
}
