package com.hms.appointment.entity;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.type.SqlTypes;

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
@Table(name = "provider_working_hours", indexes = @Index(name = "idx_hours_provider_day", columnList = "provider_id,day_of_week"))
@SQLRestriction("is_deleted = false")
public class ProviderWorkingHours extends BaseEntity {

	@JdbcTypeCode(SqlTypes.CHAR)
	@Column(name = "provider_id", columnDefinition = "CHAR(36)", nullable = false)
	private UUID providerId;

	@Enumerated(EnumType.STRING)
	@Column(name = "day_of_week", nullable = false, length = 10)
	private DayOfWeek dayOfWeek;

	@Column(name = "start_time", nullable = false)
	private LocalTime startTime;

	@Column(name = "end_time", nullable = false)
	private LocalTime endTime;

	@Column(name = "lunch_start")
	private LocalTime lunchStart;

	@Column(name = "lunch_end")
	private LocalTime lunchEnd;
}
