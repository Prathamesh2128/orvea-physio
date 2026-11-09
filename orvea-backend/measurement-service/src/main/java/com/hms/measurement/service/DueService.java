package com.hms.measurement.service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hms.common.security.ClinicScope;
import com.hms.measurement.client.AppointmentClient;
import com.hms.measurement.client.AppointmentClient.NextVisit;
import com.hms.measurement.dto.DueDtos.DueItem;
import com.hms.measurement.dto.DueDtos.DueList;
import com.hms.measurement.dto.DueDtos.NextVisitView;
import com.hms.measurement.engine.DueCalculator;
import com.hms.measurement.engine.ProtocolRules;
import com.hms.measurement.entity.MeasurementPlan;
import com.hms.measurement.entity.MeasurementSettings;
import com.hms.measurement.enums.DueBucket;
import com.hms.measurement.enums.PlanStatus;
import com.hms.measurement.repository.PlanRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class DueService {

	private final PlanRepository plans;
	private final SettingsService settingsService;
	private final TrendService trendService;
	private final AppointmentClient appointmentClient;
	private final Clock clock;

	/**
	 * The therapist's "Measurements due" screen. therapistId null = everyone in the
	 * clinic.
	 */
	@Transactional
	public DueList due(UUID therapistId) {
		UUID clinic = ClinicScope.currentClinicId();
		LocalDate today = LocalDate.now(clock);
		MeasurementSettings s = settingsService.effective(clinic);
		List<MeasurementPlan> candidates = plans.findDue(PlanStatus.ACTIVE, clinic, therapistId,
				today.plusDays(s.getComingUpDays()));
		Map<UUID, NextVisit> visits = appointmentClient.nextVisits(candidates.stream()
				.map(MeasurementPlan::getPatientId).collect(Collectors.toCollection(LinkedHashSet::new)));

		List<DueItem> overdue = new ArrayList<>(), dueToday = new ArrayList<>(), comingUp = new ArrayList<>();
		for (MeasurementPlan p : candidates) {
			DueBucket bucket = DueCalculator.bucket(p.getNextDueDate(), today, s.getComingUpDays());
			if (bucket == null)
				continue;
			NextVisit v = visits.get(p.getPatientId());
			DueItem item = new DueItem(p.getId(), p.getPatientId(), p.getPatientUhid(), p.getPatientName(),
					p.getCaseId(), p.getCaseNumber(), p.getTherapistId(), p.getKind(),
					ProtocolRules.describe(p.getKind(), p.getLimbType(), p.getStepCm(), p.getStartLandmark(),
							p.getPointCount()),
					p.getAffectedSide(), p.getNextDueDate(),
					bucket == DueBucket.OVERDUE ? ChronoUnit.DAYS.between(p.getNextDueDate(), today) : 0, bucket,
					trendService.lastResult(p, s),
					v == null ? null : new NextVisitView(v.date(), v.startTime(), v.providerName()),
					"/api/v1/measurements/plans/" + p.getId() + "/form");
			switch (bucket) {
			case OVERDUE -> overdue.add(item);
			case DUE_TODAY -> dueToday.add(item);
			case COMING_UP -> comingUp.add(item);
			}
		}
		overdue.sort(Comparator.comparing(DueItem::dueDate)); // longest overdue first
		dueToday.sort(Comparator.comparing(
				(DueItem i) -> i.nextVisit() != null && today.equals(i.nextVisit().date()) ? i.nextVisit().startTime()
						: LocalTime.MAX)
				.thenComparing(i -> String.valueOf(i.patientName())));
		comingUp.sort(Comparator.comparing(DueItem::dueDate));
		return new DueList(today, s.getComingUpDays(), overdue, dueToday, comingUp);
	}
}