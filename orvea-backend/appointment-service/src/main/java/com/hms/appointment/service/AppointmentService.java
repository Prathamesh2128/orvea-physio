package com.hms.appointment.service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hms.appointment.client.PatientClient;
import com.hms.appointment.client.PatientRef;
import com.hms.appointment.client.StaffClient;
import com.hms.appointment.client.StaffRef;
import com.hms.appointment.dto.AppointmentResponse;
import com.hms.appointment.dto.BookAppointmentRequest;
import com.hms.appointment.dto.RescheduleRequest;
import com.hms.appointment.entity.Appointment;
import com.hms.appointment.entity.ClinicSchedulingSettings;
import com.hms.appointment.entity.VisitType;
import com.hms.appointment.enums.AppointmentStatus;
import com.hms.appointment.enums.BookingSource;
import com.hms.appointment.event.AppointmentEvents;
import com.hms.appointment.repository.AppointmentRepository;
import com.hms.appointment.repository.SlotLockRepository;
import com.hms.appointment.service.ProviderScheduleService.DayContext;
import com.hms.appointment.util.AppointmentMapper;
import com.hms.appointment.util.SlotCalculator;
import com.hms.appointment.util.SlotCalculator.Interval;
import com.hms.appointment.util.SlotCalculator.Violation;
import com.hms.common.api.PageResponse;
import com.hms.common.exception.BusinessException;
import com.hms.common.exception.DuplicateResourceException;
import com.hms.common.exception.ResourceNotFoundException;
import com.hms.common.security.ClinicScope;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AppointmentService {

	private final AppointmentRepository appointmentRepository;
	private final VisitTypeService visitTypeService;
	private final SlotLockRepository slotLockRepository;
	private final ProviderScheduleService scheduleService;
	private final SchedulingSettingsService settingsService;
	private final ReminderService reminderService;
	private final PatientClient patientClient;
	private final StaffClient staffClient;
	private final AppointmentEvents events;
	private final Clock clock;

	// ------------------------------------------------------------ book
	@Transactional
	public AppointmentResponse book(BookAppointmentRequest req) {
		return bookInternal(req, null);
	}

	private AppointmentResponse bookInternal(BookAppointmentRequest req, Appointment rescheduledFrom) {
		UUID clinicId = ClinicScope.currentClinicId();

		VisitType vt = visitTypeService.require(req.visitTypeId());
		if (!vt.isActive())
			throw new BusinessException("Visit type is not active");
		PatientRef patient = patientClient.get(req.patientId());
		if (!patient.active())
			throw new BusinessException("Patient record is inactive");
		StaffRef provider = staffClient.requireBookable(req.providerId());

		ClinicSchedulingSettings settings = settingsService.effective(clinicId);
		LocalDate today = LocalDate.now(clock);
		if (req.date().isBefore(today))
			throw new BusinessException("Appointments cannot be booked in the past");
		if (req.date().isAfter(today.plusDays(settings.getMaxAdvanceBookingDays()))) {
			throw new BusinessException(
					"Appointments can be booked up to " + settings.getMaxAdvanceBookingDays() + " days ahead");
		}

		DayContext ctx = scheduleService.dayContext(req.providerId(), req.date());

		// Serialise every booking for this provider-day, then check overlaps inside the
		// lock
		String lockKey = req.providerId() + ":" + req.date();
		slotLockRepository.initIfAbsent(lockKey);
		slotLockRepository.findForUpdate(lockKey);

		List<Interval> bookings = appointmentRepository
				.findBlocking(req.providerId(), req.date(), AppointmentStatus.BLOCKING).stream()
				.map(a -> new Interval(a.getStartTime(), a.getEndTime())).toList();

		BookingSource source = req.source() == null ? BookingSource.PHONE : req.source();
		LocalTime notBefore = req.date().equals(today) && source != BookingSource.WALK_IN
				? LocalTime.now(clock).truncatedTo(ChronoUnit.MINUTES)
				: null;

		Violation v = SlotCalculator.violation(req.startTime(), vt.getDurationMinutes(), ctx.rows(),
				ctx.profile().getSlotMinutes(), ctx.profile().getMaxParallel(), ctx.leaves(), bookings, notBefore);
		if (v == Violation.SLOT_FULL)
			throw new DuplicateResourceException(v.message());
		if (v != null)
			throw new BusinessException(v.message());

		LocalTime end = req.startTime().plusMinutes(vt.getDurationMinutes());
		if (appointmentRepository.countPatientOverlap(req.patientId(), req.date(), req.startTime(), end,
				AppointmentStatus.BLOCKING) > 0) {
			throw new DuplicateResourceException("This patient already has an appointment at that time");
		}

		Appointment a = new Appointment();
		a.setClinicId(clinicId);
		a.setPatientId(patient.id());
		a.setPatientUhid(patient.uhid());
		a.setPatientName(patient.fullName());
		a.setPatientPhone(patient.phone());
		a.setProviderId(provider.id());
		a.setProviderName(provider.displayName());
		a.setVisitType(vt);
		a.setCaseId(req.caseId());
		a.setAppointmentDate(req.date());
		a.setStartTime(req.startTime());
		a.setEndTime(end);
		a.setStatus(AppointmentStatus.BOOKED);
		a.setSource(source);
		a.setNote(req.note());
		if (rescheduledFrom != null)
			a.setRescheduledFromId(rescheduledFrom.getId());
		a = appointmentRepository.save(a);

		reminderService.scheduleFor(a);

		if (rescheduledFrom == null)
			events.booked(a);
		else
			events.rescheduled(rescheduledFrom, a);

		return AppointmentMapper.toResponse(a);
	}

	// -------------------------------------------------- reschedule
	@Transactional
	public AppointmentResponse reschedule(UUID id, RescheduleRequest req) {
		Appointment old = load(id);
		if (!old.getStatus().canMoveTo(AppointmentStatus.RESCHEDULED)) {
			throw new BusinessException("A " + old.getStatus() + " appointment cannot be rescheduled");
		}
		old.setStatus(AppointmentStatus.RESCHEDULED); // frees the old time before the new check
		appointmentRepository.saveAndFlush(old);
		reminderService.cancelPending(old.getId());

		BookAppointmentRequest next = new BookAppointmentRequest(old.getPatientId(),
				req.providerId() != null ? req.providerId() : old.getProviderId(), old.getVisitType().getId(),
				old.getCaseId(), req.date(), req.startTime(), old.getSource(),
				req.note() != null ? req.note() : old.getNote());
		return bookInternal(next, old); // any failure rolls the whole transaction back, old stays booked
	}

	// ------------------------------------------------------ status
	@Transactional
	public AppointmentResponse changeStatus(UUID id, AppointmentStatus target) {
		if (target == AppointmentStatus.CANCELLED || target == AppointmentStatus.RESCHEDULED) {
			throw new BusinessException("Use the cancel or reschedule action for " + target);
		}
		Appointment a = load(id);
		if (!a.getStatus().canMoveTo(target)) {
			throw new BusinessException("Cannot change appointment from " + a.getStatus() + " to " + target);
		}
		a.setStatus(target);
		if (target != AppointmentStatus.CONFIRMED)
			reminderService.cancelPending(a.getId());

		AppointmentStatus from = a.getStatus();
		a.setStatus(target);
		events.statusChanged(a, from, target);
		
		if (target == AppointmentStatus.COMPLETED)
			events.completed(a);
		return AppointmentMapper.toResponse(a);
	}

	@Transactional
	public AppointmentResponse cancel(UUID id, String reason) {
		if (reason == null || reason.isBlank())
			throw new BusinessException("A cancellation reason is required");
		Appointment a = load(id);
		if (!a.getStatus().canMoveTo(AppointmentStatus.CANCELLED)) {
			throw new BusinessException("A " + a.getStatus() + " appointment cannot be cancelled");
		}
		a.setStatus(AppointmentStatus.CANCELLED);
		a.setCancellationReason(reason.trim());
		a.setCancelledAt(LocalDateTime.now(clock));
		reminderService.cancelPending(a.getId());
		events.cancelled(a);
		return AppointmentMapper.toResponse(a);
	}

	// ------------------------------------------------------- read
	@Transactional(readOnly = true)
	public AppointmentResponse get(UUID id) {
		return AppointmentMapper.toResponse(load(id));
	}

	@Transactional(readOnly = true)
	public PageResponse<AppointmentResponse> search(LocalDate date, UUID providerId, UUID patientId,
			AppointmentStatus status, Pageable pageable) {
		return PageResponse.from(appointmentRepository.search(date, providerId, patientId, status,
				ClinicScope.currentClinicId(), pageable), AppointmentMapper::toResponse);
	}

	private Appointment load(UUID id) {
		Appointment a = appointmentRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Appointment", id));
		ClinicScope.assertSameClinic(a.getClinicId(), "Appointment", id);
		return a;
	}
}
