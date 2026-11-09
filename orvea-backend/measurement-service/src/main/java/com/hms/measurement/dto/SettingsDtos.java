package com.hms.measurement.dto;

import java.math.BigDecimal;

import com.hms.measurement.enums.DueMode;
import com.hms.measurement.enums.TrendMetric;

public final class SettingsDtos {
	private SettingsDtos() {
	}

	/**
	 * Only what is sent is changed. flagThresholdPercent is cleared with
	 * clearFlagThreshold=true.
	 */
	public record SettingsRequest(Integer defaultRepeatDays, DueMode defaultDueMode, Integer comingUpDays,
			TrendMetric trendMetric, BigDecimal trendTolerancePercent, BigDecimal flagThresholdPercent,
			Boolean clearFlagThreshold, Integer patientReminderLeadDays) {
	}

	public record SettingsResponse(int defaultRepeatDays, DueMode defaultDueMode, int comingUpDays,
			TrendMetric trendMetric, BigDecimal trendTolerancePercent, BigDecimal flagThresholdPercent,
			int patientReminderLeadDays) {
	}
}