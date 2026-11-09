package com.hms.measurement.dto;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import com.hms.measurement.enums.DueBucket;
import com.hms.measurement.enums.MeasurementKind;
import com.hms.measurement.enums.Side;

public final class DueDtos {
	private DueDtos() {
	}

	public record NextVisitView(LocalDate date, LocalTime startTime, String providerName) {
	}

	public record DueItem(UUID planId, UUID patientId, String patientUhid, String patientName, UUID caseId,
			String caseNumber, UUID therapistId, MeasurementKind kind, String whatToMeasure, Side affectedSide,
			LocalDate dueDate, long daysOverdue, DueBucket bucket, PlanDtos.LastResult lastResult,
			NextVisitView nextVisit, String measureNow) {
	}

	public record DueList(LocalDate asOf, int comingUpDays, List<DueItem> overdue, List<DueItem> dueToday,
			List<DueItem> comingUp) {
	}
}