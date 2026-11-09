package com.hms.measurement.engine;

import java.time.LocalDate;

import com.hms.common.exception.BusinessException;
import com.hms.measurement.enums.DueBucket;
import com.hms.measurement.enums.DueMode;

public final class DueCalculator {
	private DueCalculator() {
	}

	public static final int MAX_SNOOZE_DAYS = 30;

	/**
	 * FROM_ACTUAL: measurement date + interval. FROM_PLANNED: keep the planned
	 * rhythm; step forward by whole intervals until after the measurement date.
	 */
	public static LocalDate afterMeasurement(DueMode mode, LocalDate previousDue, LocalDate measuredOn,
			int repeatDays) {
		if (mode == DueMode.FROM_PLANNED && previousDue != null)
			return advancePast(previousDue, measuredOn, repeatDays);
		return measuredOn.plusDays(repeatDays);
	}

	/** "Not needed" for this occurrence. */
	public static LocalDate afterSkip(DueMode mode, LocalDate previousDue, LocalDate today, int repeatDays) {
		if (mode == DueMode.FROM_PLANNED && previousDue != null)
			return advancePast(previousDue, today, repeatDays);
		return today.plusDays(repeatDays);
	}

	private static LocalDate advancePast(LocalDate from, LocalDate after, int repeatDays) {
		LocalDate d = from;
		do {
			d = d.plusDays(repeatDays);
		} while (!d.isAfter(after));
		return d;
	}

	public static LocalDate checkSnooze(LocalDate until, LocalDate today) {
		if (until == null || !until.isAfter(today))
			throw new BusinessException("Snooze until a future date");
		if (until.isAfter(today.plusDays(MAX_SNOOZE_DAYS))) {
			throw new BusinessException("A measurement can be snoozed for at most " + MAX_SNOOZE_DAYS + " days");
		}
		return until;
	}

	/** null = not due yet (outside the "coming up" window). */
	public static DueBucket bucket(LocalDate due, LocalDate today, int comingUpDays) {
		if (due == null)
			return null;
		if (due.isBefore(today))
			return DueBucket.OVERDUE;
		if (due.isEqual(today))
			return DueBucket.DUE_TODAY;
		return !due.isAfter(today.plusDays(comingUpDays)) ? DueBucket.COMING_UP : null;
	}
}