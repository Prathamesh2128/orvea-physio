package com.hms.measurement.engine;

import java.math.BigDecimal;
import java.util.List;

import com.hms.measurement.enums.Landmark;
import com.hms.measurement.enums.LimbType;
import com.hms.measurement.enums.MeasurementKind;

/** The protocol exactly as it was when the measurement was taken. */
public record ProtocolSnapshot(MeasurementKind kind, LimbType limbType, BigDecimal stepCm, Landmark startLandmark,
		Integer pointCount, int revision, List<BigDecimal> plannedDistances) {
}