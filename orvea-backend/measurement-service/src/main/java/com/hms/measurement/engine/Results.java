package com.hms.measurement.engine;

import java.math.BigDecimal;
import java.util.List;

import com.hms.measurement.enums.AngleMethod;
import com.hms.measurement.enums.Side;

/**
 * Everything about one measurement, computed once and stored (so history never
 * changes if formulas do).
 */
public record Results(List<Item> items, Summary summary, List<Angle> angles, Side affectedSide,
		boolean percentAvailable) {

	/** One point (limb) or one landmark (face). */
	public record Item(String key, String label, Integer point, BigDecimal plannedDistanceCm,
			BigDecimal actualDistanceCm, BigDecimal leftCm, BigDecimal rightCm, BigDecimal affectedCm,
			BigDecimal unaffectedCm, BigDecimal diffCm, BigDecimal diffPercent, BigDecimal leftMinusRightCm,
			BigDecimal distanceDeviationCm) {
	}

	public record Summary(BigDecimal meanDiffCm, BigDecimal maxDiffCm, BigDecimal meanDiffPercent,
			BigDecimal maxDiffPercent, int itemsCompared) {
	}

	public record Angle(String key, String label, BigDecimal degrees, AngleMethod method, BigDecimal suggestedDegrees) {
	}
}