package com.hms.measurement.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.hms.measurement.engine.Results;
import com.hms.measurement.enums.DueMode;
import com.hms.measurement.enums.Landmark;
import com.hms.measurement.enums.LimbType;
import com.hms.measurement.enums.MeasurementKind;
import com.hms.measurement.enums.PlanEventType;
import com.hms.measurement.enums.PlanStatus;
import com.hms.measurement.enums.Side;
import com.hms.measurement.enums.TrendTag;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public final class PlanDtos {
	private PlanDtos() {
	}

	/**
	 * For LIMB give templateId, or limbType + stepCm + startLandmark + pointCount.
	 * FACE needs none of these.
	 */
	public record CreatePlanRequest(@NotNull UUID patientId, @NotNull UUID caseId, @NotNull MeasurementKind kind,
			UUID templateId, LimbType limbType, BigDecimal stepCm, Landmark startLandmark, Integer pointCount,
			Side affectedSide, Integer repeatDays, DueMode dueMode, Boolean remindPatient, LocalDate firstDueDate,
			UUID therapistId, @Size(max = 500) String note) {
	}

	public record UpdatePlanRequest(Integer repeatDays, DueMode dueMode, Boolean remindPatient, LocalDate nextDueDate,
			UUID therapistId, Side affectedSide, @Size(max = 500) String note) {
	}

	/** confirmChange + reason are required once readings exist. */
	public record ChangeProtocolRequest(BigDecimal stepCm, Landmark startLandmark, Integer pointCount,
			@Size(max = 255) String reason, Boolean confirmChange) {
	}

	public record ReasonRequest(@NotBlank @Size(max = 255) String reason) {
	}

	public record SnoozeRequest(@NotNull LocalDate until, @NotBlank @Size(max = 255) String reason) {
	}

	public record ProtocolView(MeasurementKind kind, LimbType limbType, BigDecimal stepCm, Landmark startLandmark,
			Integer pointCount, int revision, List<BigDecimal> plannedDistancesCm, String description) {
	}

	public record ScheduleView(int repeatDays, DueMode dueMode, LocalDate nextDueDate, LocalDate lastMeasuredOn,
			boolean remindPatient) {
	}

	public record LastResult(UUID recordId, LocalDate measuredOn, BigDecimal meanDiffPercent, BigDecimal maxDiffPercent,
			BigDecimal meanDiffCm, BigDecimal maxDiffCm, boolean flagged, TrendTag trend, int protocolRevision) {
	}

	public record PlanResponse(UUID id, UUID patientId, String patientUhid, String patientName, UUID caseId,
			String caseNumber, UUID therapistId, PlanStatus status, Side affectedSide, ProtocolView protocol,
			ScheduleView schedule, LastResult lastResult, String note, LocalDate startedOn) {
	}

	public record FormItem(String key, String label, Integer point, BigDecimal plannedDistanceCm) {
	}

	/**
	 * "Measure now": the saved protocol, the default side and the previous values
	 * for reference.
	 */
	public record PlanForm(UUID planId, MeasurementKind kind, Side affectedSide, int protocolRevision,
			String description, List<FormItem> items, List<FormItem> angles, Results lastValues) {
	}

	public record EventView(UUID id, PlanEventType type, LocalDate eventDate, String reason,
			Map<String, Object> details, String actor, LocalDateTime createdAt) {
	}
}