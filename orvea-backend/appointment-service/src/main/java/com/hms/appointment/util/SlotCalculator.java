package com.hms.appointment.util;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import com.hms.appointment.enums.SlotState;

public final class SlotCalculator {
	private SlotCalculator() {
	}

	public enum Violation {
		OUTSIDE_WORKING_HOURS("Selected time is outside the provider's working hours"),
		NOT_ON_GRID("Start time does not match the provider's slot schedule"),
		LUNCH_BREAK("Selected time overlaps the provider's lunch break"),
		PROVIDER_ON_LEAVE("Provider is on leave at the selected time"), TIME_PASSED("Selected time has already passed"),
		SLOT_FULL("This slot is already booked");

		private final String message;

		Violation(String message) {
			this.message = message;
		}

		public String message() {
			return message;
		}
	}

	public record Interval(LocalTime start, LocalTime end) {
		public boolean overlaps(Interval o) {
			return start.isBefore(o.end) && end.isAfter(o.start);
		}
	}

	/** A working block with an optional lunch break inside it. */
	public record Row(LocalTime start, LocalTime end, Interval lunch) {
	}

	public record Cell(LocalTime start, LocalTime end, SlotState state, int booked, int capacity) {
	}

	private static int min(LocalTime t) {
		return t.getHour() * 60 + t.getMinute();
	}

	private static LocalTime time(int minutes) {
		return LocalTime.of(minutes / 60, minutes % 60);
	}

	/**
	 * Highest number of bookings running at the same moment inside the interval.
	 */
	public static int maxConcurrency(Interval iv, List<Interval> bookings) {
		List<LocalTime> points = new ArrayList<>();
		points.add(iv.start());
		for (Interval b : bookings) {
			if (!b.start().isBefore(iv.start()) && b.start().isBefore(iv.end()))
				points.add(b.start());
		}
		int max = 0;
		for (LocalTime p : points) {
			int c = 0;
			for (Interval b : bookings) {
				if (!b.start().isAfter(p) && b.end().isAfter(p))
					c++;
			}
			max = Math.max(max, c);
		}
		return max;
	}

	/** @return null when the visit fits, otherwise the first rule it breaks. */
	public static Violation violation(LocalTime start, int duration, List<Row> rows, int slotMinutes, int maxParallel,
			List<Interval> leaves, List<Interval> bookings, LocalTime notBefore) {
		int s = min(start);
		int e = s + duration;
		Row row = null;
		for (Row r : rows) {
			if (s >= min(r.start()) && e <= min(r.end())) {
				row = r;
				break;
			}
		}
		if (row == null)
			return Violation.OUTSIDE_WORKING_HOURS;
		if ((s - min(row.start())) % slotMinutes != 0)
			return Violation.NOT_ON_GRID;

		Interval visit = new Interval(start, time(e));
		if (row.lunch() != null && row.lunch().overlaps(visit))
			return Violation.LUNCH_BREAK;
		for (Interval leave : leaves) {
			if (leave.overlaps(visit))
				return Violation.PROVIDER_ON_LEAVE;
		}
		if (notBefore != null && start.isBefore(notBefore))
			return Violation.TIME_PASSED;
		if (maxConcurrency(visit, bookings) >= maxParallel)
			return Violation.SLOT_FULL;
		return null;
	}

	/**
	 * Every start time (on the provider grid) where a visit of this length can be
	 * booked.
	 */
	public static List<LocalTime> availableStarts(List<Row> rows, int slotMinutes, int duration, int maxParallel,
			List<Interval> leaves, List<Interval> bookings, LocalTime notBefore) {
		List<LocalTime> out = new ArrayList<>();
		for (Row r : rows) {
			for (int s = min(r.start()); s + duration <= min(r.end()); s += slotMinutes) {
				if (violation(time(s), duration, rows, slotMinutes, maxParallel, leaves, bookings, notBefore) == null) {
					out.add(time(s));
				}
			}
		}
		return out;
	}

	/** The day board: one cell per grid slot with its state. */
	public static List<Cell> dayGrid(List<Row> rows, int slotMinutes, int maxParallel, List<Interval> leaves,
			List<Interval> bookings, LocalTime notBefore) {
		List<Cell> cells = new ArrayList<>();
		for (Row r : rows) {
			for (int s = min(r.start()); s + slotMinutes <= min(r.end()); s += slotMinutes) {
				Interval cell = new Interval(time(s), time(s + slotMinutes));
				int booked = maxConcurrency(cell, bookings);
				SlotState state;
				if (notBefore != null && !cell.end().isAfter(notBefore))
					state = SlotState.PAST;
				else if (leaves.stream().anyMatch(cell::overlaps))
					state = SlotState.LEAVE;
				else if (r.lunch() != null && r.lunch().overlaps(cell))
					state = SlotState.LUNCH;
				else
					state = booked >= maxParallel ? SlotState.BOOKED : SlotState.AVAILABLE;
				cells.add(new Cell(cell.start(), cell.end(), state, booked, maxParallel));
			}
		}
		return cells;
	}
}