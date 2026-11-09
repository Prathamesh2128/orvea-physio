package com.hms.measurement.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.hms.common.security.SecurityUtils;
import com.hms.measurement.dto.PlanDtos.EventView;
import com.hms.measurement.entity.MeasurementPlan;
import com.hms.measurement.entity.PlanEvent;
import com.hms.measurement.enums.PlanEventType;
import com.hms.measurement.repository.PlanEventRepository;
import com.hms.measurement.util.Json;

import lombok.RequiredArgsConstructor;

/** Writes and reads a plan's history (including snooze / skip reasons). */
@Component
@RequiredArgsConstructor
public class PlanEvents {

	private final PlanEventRepository repository;

	public void log(MeasurementPlan plan, PlanEventType type, LocalDate date, String reason,
			Map<String, Object> details) {
		PlanEvent e = new PlanEvent();
		e.setClinicId(plan.getClinicId());
		e.setPlanId(plan.getId());
		e.setType(type);
		e.setEventDate(date);
		e.setReason(reason);
		e.setDetails(details == null || details.isEmpty() ? null : Json.write(details));
		e.setActor(SecurityUtils.currentUsername());
		repository.save(e);
	}

	public List<EventView> list(UUID planId) {
		return repository.findByPlanIdOrderByCreatedAtDesc(planId).stream()
				.map(e -> new EventView(e.getId(), e.getType(), e.getEventDate(), e.getReason(),
						e.getDetails() == null ? null : Json.readMap(e.getDetails()), e.getActor(), e.getCreatedAt()))
				.toList();
	}
}