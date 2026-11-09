package com.hms.measurement.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hms.measurement.dto.PlanDtos.LastResult;
import com.hms.measurement.engine.TrendCalculator;
import com.hms.measurement.entity.MeasurementPlan;
import com.hms.measurement.entity.MeasurementRecord;
import com.hms.measurement.entity.MeasurementSettings;
import com.hms.measurement.enums.RecordStatus;
import com.hms.measurement.enums.TrendTag;
import com.hms.measurement.repository.RecordRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TrendService {

	private final RecordRepository records;

	/**
	 * The plan's latest valid reading with its trend, or null if nothing has been
	 * measured.
	 */
	@Transactional(readOnly = true)
	public LastResult lastResult(MeasurementPlan plan, MeasurementSettings settings) {
		List<MeasurementRecord> latest = records.findRecent(plan.getId(), RecordStatus.ACTIVE, PageRequest.of(0, 1));
		return latest.isEmpty() ? null : toLastResult(latest.get(0), settings);
	}

	public LastResult toLastResult(MeasurementRecord r, MeasurementSettings s) {
		return new LastResult(r.getId(), r.getMeasuredOn(), r.getMeanDiffPercent(), r.getMaxDiffPercent(),
				r.getMeanDiffCm(), r.getMaxDiffCm(), r.isFlagged(), trendFor(r, s), r.getProtocolRevision());
	}

	/**
	 * Compared with the previous reading taken under the SAME protocol revision.
	 */
	@Transactional(readOnly = true)
	public TrendTag trendFor(MeasurementRecord r, MeasurementSettings s) {
		List<MeasurementRecord> previous = records.findPrevious(r.getPlanId(), RecordStatus.ACTIVE,
				r.getProtocolRevision(), r.getId(), r.getMeasuredOn(),
				r.getCreatedAt() == null ? LocalDateTime.now().plusYears(1) : r.getCreatedAt(), PageRequest.of(0, 1));
		if (previous.isEmpty())
			return TrendTag.NOT_ENOUGH_DATA;
		MeasurementRecord p = previous.get(0);
		return TrendCalculator.tag(
				TrendCalculator.metric(s.getTrendMetric(), r.getMeanDiffPercent(), r.getMaxDiffPercent()),
				TrendCalculator.metric(s.getTrendMetric(), p.getMeanDiffPercent(), p.getMaxDiffPercent()),
				s.getTrendTolerancePercent());
	}
}