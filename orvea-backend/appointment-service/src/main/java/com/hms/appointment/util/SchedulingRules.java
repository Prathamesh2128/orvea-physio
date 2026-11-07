package com.hms.appointment.util;

import java.util.Set;
import java.util.TreeSet;

import com.hms.common.exception.BusinessException;

public final class SchedulingRules {
	private SchedulingRules() {
	}

	public static final Set<Integer> SLOT_MINUTES = Set.of(5, 10, 15, 20, 30, 45, 60);

	public static int requireSlot(int v) {
		if (!SLOT_MINUTES.contains(v)) {
			throw new BusinessException("Slot length must be one of " + new TreeSet<>(SLOT_MINUTES) + " minutes");
		}
		return v;
	}

	public static int requireMaxParallel(int v) {
		if (v < 1 || v > 10)
			throw new BusinessException("max_parallel must be between 1 and 10");
		return v;
	}

	public static int requireDuration(int v) {
		if (v < 5 || v > 240 || v % 5 != 0) {
			throw new BusinessException("Duration must be 5 to 240 minutes, in steps of 5");
		}
		return v;
	}
}
