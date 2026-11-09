package com.hms.measurement.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import com.hms.measurement.engine.ProtocolSnapshot;
import com.hms.measurement.engine.Results;
import com.hms.measurement.enums.AngleMethod;
import com.hms.measurement.enums.MeasurementKind;
import com.hms.measurement.enums.RecordStatus;
import com.hms.measurement.enums.Side;
import com.hms.measurement.enums.TrendTag;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public final class RecordDtos {
	private RecordDtos() {
	}

	/**
	 * actualDistanceCm is where on the limb you really measured (it may differ from
	 * the planned point).
	 */
	public record PointReading(@NotNull Integer point,
			@NotNull @DecimalMin("0.00") @DecimalMax("200.00") BigDecimal actualDistanceCm, BigDecimal leftCm,
			BigDecimal rightCm) {
	}

	public record LandmarkReading(@NotBlank String key, BigDecimal leftCm, BigDecimal rightCm) {
	}

	/**
	 * CAMERA_CONFIRMED = the clinician reviewed a camera suggestion; send both the
	 * final and the suggested value.
	 */
	public record AngleReading(@NotBlank String key, @NotNull BigDecimal degrees, AngleMethod method,
			BigDecimal suggestedDegrees) {
	}

	/**
	 * LIMB: points. FACE: landmarks (+ optional angles). affectedSide overrides the
	 * plan's default for this record.
	 */
	public record RecordMeasurementRequest(LocalDate measuredOn, LocalTime measuredTime, Side affectedSide,
			@Valid List<PointReading> points, @Valid List<LandmarkReading> landmarks, @Valid List<AngleReading> angles,
			@Size(max = 500) String notes) {
	}

	public record RecordResponse(UUID id, UUID planId, UUID patientId, UUID caseId, MeasurementKind kind,
			LocalDate measuredOn, LocalTime measuredTime, int protocolRevision, ProtocolSnapshot protocol,
			Results results, boolean flagged, TrendTag trend, RecordStatus status, String voidReason, String notes,
			boolean hasPhoto, String measuredBy, LocalDate nextDueDate, LocalDateTime createdAt) {
	}

	public record HistoryPoint(UUID recordId, LocalDate measuredOn, int protocolRevision, BigDecimal meanDiffPercent,
			BigDecimal maxDiffPercent, BigDecimal meanDiffCm, BigDecimal maxDiffCm, boolean flagged,
			List<Results.Item> items) {
	}

	public record CompareEnd(UUID recordId, LocalDate measuredOn, BigDecimal meanDiffPercent,
			BigDecimal maxDiffPercent) {
	}

	public record CompareItem(String key, String label, BigDecimal baselineAffectedCm, BigDecimal targetAffectedCm,
			BigDecimal affectedChangeCm, BigDecimal baselineUnaffectedCm, BigDecimal targetUnaffectedCm,
			BigDecimal baselineDiffPercent, BigDecimal targetDiffPercent, BigDecimal diffPercentChangePoints) {
	}

	/** Numbers only: no judgement about better or worse. */
	public record CompareResponse(UUID planId, CompareEnd baseline, CompareEnd target, long daysBetween,
			Side affectedSide, List<CompareItem> items, BigDecimal meanDiffPercentChange,
			BigDecimal maxDiffPercentChange, BigDecimal meanDiffCmChange) {
	}
}