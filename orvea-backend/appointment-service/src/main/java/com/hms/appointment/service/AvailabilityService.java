package com.hms.appointment.service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hms.appointment.dto.AppointmentResponse;
import com.hms.appointment.dto.AvailabilityResponse;
import com.hms.appointment.dto.DayScheduleResponse;
import com.hms.appointment.entity.Appointment;
import com.hms.appointment.entity.VisitType;
import com.hms.appointment.enums.AppointmentStatus;
import com.hms.appointment.repository.AppointmentRepository;
import com.hms.appointment.service.ProviderScheduleService.DayContext;
import com.hms.appointment.util.AppointmentMapper;
import com.hms.appointment.util.SlotCalculator;
import com.hms.appointment.util.SlotCalculator.Interval;
import com.hms.common.exception.BusinessException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AvailabilityService {

	private final ProviderScheduleService scheduleService;
	private final AppointmentRepository appointmentRepository;
	private final VisitTypeService visitTypeService;
	private final Clock clock;

	/**
	 * Daily schedule per therapist: booked / available / lunch / leave / past cells
	 * + the appointments.
	 */
	@Transactional(readOnly = true)
	public DayScheduleResponse daySchedule(UUID providerId, LocalDate date) {
		DayContext ctx = scheduleService.dayContext(providerId, date);
		List<Appointment> appts = appointmentRepository.findBlocking(providerId, date, AppointmentStatus.BLOCKING);
		List<Interval> bookings = appts.stream().map(a -> new Interval(a.getStartTime(), a.getEndTime())).toList();
		var cells = SlotCalculator.dayGrid(ctx.rows(), ctx.profile().getSlotMinutes(), ctx.profile().getMaxParallel(),
				ctx.leaves(), bookings, notBefore(date));
		List<AppointmentResponse> list = appts.stream().map(AppointmentMapper::toResponse).toList();
		return new DayScheduleResponse(providerId, ctx.profile().getProviderName(), date,
				ctx.profile().getSlotMinutes(), ctx.profile().getMaxParallel(), cells, list);
	}

	/** Start times that can take a visit of the chosen type. */
	@Transactional(readOnly = true)
	public AvailabilityResponse availability(UUID providerId, LocalDate date, UUID visitTypeId) {
		VisitType vt = visitTypeService.require(visitTypeId);
		if (!vt.isActive())
			throw new BusinessException("Visit type is not active");
		DayContext ctx = scheduleService.dayContext(providerId, date);
		List<Interval> bookings = appointmentRepository.findBlocking(providerId, date, AppointmentStatus.BLOCKING)
				.stream().map(a -> new Interval(a.getStartTime(), a.getEndTime())).toList();
		List<LocalTime> starts = SlotCalculator.availableStarts(ctx.rows(), ctx.profile().getSlotMinutes(),
				vt.getDurationMinutes(), ctx.profile().getMaxParallel(), ctx.leaves(), bookings, notBefore(date));
		return new AvailabilityResponse(providerId, date, visitTypeId, vt.getDurationMinutes(), starts);
	}

	private LocalTime notBefore(LocalDate date) {
		LocalDate today = LocalDate.now(clock);
		if (date.isBefore(today))
			return LocalTime.MAX;
		return date.equals(today) ? LocalTime.now(clock).truncatedTo(ChronoUnit.MINUTES) : null;
	}
}
