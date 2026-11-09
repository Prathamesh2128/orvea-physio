package com.hms.measurement.engine;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Objects;

import com.hms.measurement.enums.Side;

public final class DifferenceCalculator {
	private DifferenceCalculator() {
	}

	public record Diff(BigDecimal affectedCm, BigDecimal unaffectedCm, BigDecimal diffCm, BigDecimal diffPercent,
			BigDecimal leftMinusRightCm) {
	}

	public static BigDecimal round2(BigDecimal v) {
		return v == null ? null : v.setScale(2, RoundingMode.HALF_UP);
	}

	/**
	 * diff = affected - unaffected (positive = affected side larger); percent =
	 * diff / unaffected x 100. Without a known affected side only left - right is
	 * returned.
	 */
	public static Diff compute(Side affectedSide, BigDecimal left, BigDecimal right) {
		BigDecimal l = round2(left);
		BigDecimal r = round2(right);
		BigDecimal leftMinusRight = l != null && r != null ? l.subtract(r) : null;
		if (affectedSide == null || l == null || r == null)
			return new Diff(null, null, null, null, leftMinusRight);
		BigDecimal affected = affectedSide == Side.LEFT ? l : r;
		BigDecimal unaffected = affectedSide == Side.LEFT ? r : l;
		BigDecimal diff = affected.subtract(unaffected);
		BigDecimal percent = unaffected.signum() > 0
				? diff.multiply(BigDecimal.valueOf(100)).divide(unaffected, 2, RoundingMode.HALF_UP)
				: null;
		return new Diff(affected, unaffected, diff, percent, leftMinusRight);
	}

	/**
	 * Mean and highest difference over the items where both sides were measured.
	 */
	public static Results.Summary summarize(List<Diff> diffs) {
		List<Diff> ok = diffs.stream().filter(d -> d.diffCm() != null).toList();
		if (ok.isEmpty())
			return new Results.Summary(null, null, null, null, 0);
		List<BigDecimal> cm = ok.stream().map(Diff::diffCm).toList();
		List<BigDecimal> pct = ok.stream().map(Diff::diffPercent).filter(Objects::nonNull).toList();
		return new Results.Summary(avg(cm), cm.stream().max(BigDecimal::compareTo).orElse(null),
				pct.isEmpty() ? null : avg(pct), pct.stream().max(BigDecimal::compareTo).orElse(null), ok.size());
	}

	private static BigDecimal avg(List<BigDecimal> values) {
		return values.stream().reduce(BigDecimal.ZERO, BigDecimal::add).divide(BigDecimal.valueOf(values.size()), 2,
				RoundingMode.HALF_UP);
	}
}