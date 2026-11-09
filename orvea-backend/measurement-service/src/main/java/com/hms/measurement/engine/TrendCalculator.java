package com.hms.measurement.engine;

import java.math.BigDecimal;

import com.hms.measurement.enums.TrendMetric;
import com.hms.measurement.enums.TrendTag;

public final class TrendCalculator {
	private TrendCalculator() {
	}

	public static BigDecimal metric(TrendMetric metric, BigDecimal meanDiffPercent, BigDecimal maxDiffPercent) {
		return metric == TrendMetric.MAX_DIFF_PERCENT ? maxDiffPercent : meanDiffPercent;
	}

	/**
	 * Within +/- tolerance (percentage points) of the previous reading = NO_CHANGE.
	 */
	public static TrendTag tag(BigDecimal latest, BigDecimal previous, BigDecimal tolerance) {
		if (latest == null || previous == null)
			return TrendTag.NOT_ENOUGH_DATA;
		BigDecimal delta = latest.subtract(previous);
		BigDecimal tol = tolerance == null ? BigDecimal.ZERO : tolerance.abs();
		if (delta.abs().compareTo(tol) <= 0)
			return TrendTag.NO_CHANGE;
		return delta.signum() > 0 ? TrendTag.RISING : TrendTag.FALLING;
	}
}