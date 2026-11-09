package com.hms.measurement.engine;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.hms.common.exception.BusinessException;
import com.hms.measurement.enums.Landmark;
import com.hms.measurement.enums.LimbType;
import com.hms.measurement.enums.MeasurementKind;

public final class ProtocolRules {
	private ProtocolRules() {
	}

	public static final int MIN_POINTS = 3;
	public static final int MAX_POINTS = 6;

	private static final Map<LimbType, List<BigDecimal>> STEPS = Map.of(LimbType.ARM,
			List.of(new BigDecimal("4"), new BigDecimal("5"), new BigDecimal("6")), LimbType.LEG,
			List.of(new BigDecimal("8"), new BigDecimal("10"), new BigDecimal("12")));
	private static final Map<LimbType, List<Landmark>> LANDMARKS = Map.of(LimbType.ARM,
			List.of(Landmark.WRIST, Landmark.ELBOW_CREASE), LimbType.LEG,
			List.of(Landmark.ANKLE_BONE, Landmark.KNEE_CAP));

	public static List<BigDecimal> allowedSteps(LimbType t) {
		return STEPS.get(t);
	}

	public static List<Landmark> allowedLandmarks(LimbType t) {
		return LANDMARKS.get(t);
	}

	public static void validate(LimbType type, BigDecimal step, Landmark landmark, Integer points) {
		if (type == null)
			throw new BusinessException("Choose arm or leg");
		String name = type.name().toLowerCase();
		if (step == null || STEPS.get(type).stream().noneMatch(s -> s.compareTo(step) == 0)) {
			throw new BusinessException("For a " + name + " the step must be one of: "
					+ STEPS.get(type).stream().map(BigDecimal::toPlainString).collect(Collectors.joining(", "))
					+ " cm");
		}
		if (landmark == null || !LANDMARKS.get(type).contains(landmark)) {
			throw new BusinessException("For a " + name + " the start landmark must be one of: "
					+ LANDMARKS.get(type).stream().map(Landmark::label).collect(Collectors.joining(", ")));
		}
		if (points == null || points < MIN_POINTS || points > MAX_POINTS) {
			throw new BusinessException("The number of points must be between " + MIN_POINTS + " and " + MAX_POINTS);
		}
	}

	/** Distances from the start landmark: 0, step, 2 x step ... */
	public static List<BigDecimal> plannedDistances(BigDecimal step, int points) {
		List<BigDecimal> out = new ArrayList<>();
		for (int i = 0; i < points; i++)
			out.add(step.multiply(BigDecimal.valueOf(i)).setScale(2));
		return out;
	}

	public static String pointLabel(Landmark landmark, BigDecimal plannedDistance) {
		return plannedDistance.signum() == 0 ? "At the " + landmark.label()
				: plannedDistance.stripTrailingZeros().toPlainString() + " cm from the " + landmark.label();
	}

	public static String describe(MeasurementKind kind, LimbType type, BigDecimal step, Landmark landmark,
			Integer points) {
		if (kind == MeasurementKind.FACE)
			return "Face: 2 distances and 2 angles, both sides";
		String limb = type.name().charAt(0) + type.name().substring(1).toLowerCase();
		return limb + ": " + points + " points, every " + step.stripTrailingZeros().toPlainString() + " cm from the "
				+ landmark.label();
	}
}