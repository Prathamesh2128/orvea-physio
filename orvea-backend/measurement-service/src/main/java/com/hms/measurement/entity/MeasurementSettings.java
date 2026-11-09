package com.hms.measurement.entity;

import java.math.BigDecimal;

import org.hibernate.annotations.SQLRestriction;

import com.hms.common.entity.BaseEntity;
import com.hms.measurement.enums.DueMode;
import com.hms.measurement.enums.TrendMetric;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Clinic-wide defaults and display rules. All values are editable; nothing here
 * is a clinical rule.
 */
@SuppressWarnings("serial")
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "measurement_settings")
@SQLRestriction("is_deleted = false")
public class MeasurementSettings extends BaseEntity {

	@Column(name = "default_repeat_days", nullable = false)
	private int defaultRepeatDays = 5;

	@Enumerated(EnumType.STRING)
	@Column(name = "default_due_mode", nullable = false, length = 15)
	private DueMode defaultDueMode = DueMode.FROM_ACTUAL;

	@Column(name = "coming_up_days", nullable = false)
	private int comingUpDays = 3;

	@Enumerated(EnumType.STRING)
	@Column(name = "trend_metric", nullable = false, length = 20)
	private TrendMetric trendMetric = TrendMetric.MEAN_DIFF_PERCENT;

	/** Changes smaller than this (percentage points) show as NO_CHANGE. */
	@Column(name = "trend_tolerance_percent", nullable = false, precision = 5, scale = 2)
	private BigDecimal trendTolerancePercent = new BigDecimal("0.50");

	/** Empty by default = no flagging until the clinic decides on a level. */
	@Column(name = "flag_threshold_percent", precision = 6, scale = 2)
	private BigDecimal flagThresholdPercent;

	@Column(name = "patient_reminder_lead_days", nullable = false)
	private int patientReminderLeadDays = 1;
}