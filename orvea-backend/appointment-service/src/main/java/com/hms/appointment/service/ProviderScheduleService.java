package com.hms.appointment.service;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hms.appointment.client.StaffClient;
import com.hms.appointment.client.StaffRef;
import com.hms.appointment.dto.LeaveRequest;
import com.hms.appointment.dto.LeaveResponse;
import com.hms.appointment.dto.ProviderSettingsRequest;
import com.hms.appointment.dto.ProviderSettingsResponse;
import com.hms.appointment.dto.WorkingHoursRequest;
import com.hms.appointment.dto.WorkingHoursResponse;
import com.hms.appointment.entity.ClinicSchedulingSettings;
import com.hms.appointment.entity.ProviderLeave;
import com.hms.appointment.entity.ProviderProfile;
import com.hms.appointment.entity.ProviderWorkingHours;
import com.hms.appointment.enums.AppointmentStatus;
import com.hms.appointment.enums.LeaveType;
import com.hms.appointment.repository.AppointmentRepository;
import com.hms.appointment.repository.ProviderLeaveRepository;
import com.hms.appointment.repository.ProviderProfileRepository;
import com.hms.appointment.repository.ProviderWorkingHoursRepository;
import com.hms.appointment.util.SchedulingRules;
import com.hms.appointment.util.SlotCalculator.Interval;
import com.hms.appointment.util.SlotCalculator.Row;
import com.hms.common.exception.BusinessException;
import com.hms.common.exception.ResourceNotFoundException;
import com.hms.common.security.ClinicScope;
import com.hms.common.security.SecurityUtils;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ProviderScheduleService {

	public record DayContext(ProviderProfile profile, List<Row> rows, List<Interval> leaves) {
	}

	private final ProviderProfileRepository profileRepository;
	private final ProviderWorkingHoursRepository hoursRepository;
	private final ProviderLeaveRepository leaveRepository;
	private final AppointmentRepository appointmentRepository;
	private final SchedulingSettingsService settingsService;
	private final StaffClient staffClient;
	private final Clock clock;

	// ------------------------------------------------------- settings
	@Transactional
	public ProviderSettingsResponse upsertSettings(UUID providerId, ProviderSettingsRequest req) {
		ProviderProfile p = ensureProfile(providerId);
		if (req.slotMinutes() != null)
			p.setSlotMinutes(SchedulingRules.requireSlot(req.slotMinutes()));
		if (req.maxParallel() != null)
			p.setMaxParallel(SchedulingRules.requireMaxParallel(req.maxParallel()));
		if (req.active() != null)
			p.setActive(req.active());
		return toResponse(profileRepository.save(p));
	}

	@Transactional(readOnly = true)
	public ProviderSettingsResponse getSettings(UUID providerId) {
		return toResponse(requireProfile(providerId));
	}

	// ---------------------------------------------------- working hours
	@Transactional
	public List<WorkingHoursResponse> replaceWorkingHours(UUID providerId, List<WorkingHoursRequest> rows) {
		validateRows(rows);
		ensureProfile(providerId);
		LocalDateTime at = LocalDateTime.now(clock);
		String by = SecurityUtils.currentUsername();
		hoursRepository.findByProviderIdOrderByDayOfWeekAscStartTimeAsc(providerId).forEach(h -> h.softDelete(by, at));

		UUID clinicId = requireProfile(providerId).getClinicId();
		List<WorkingHoursResponse> out = new ArrayList<>();
		for (WorkingHoursRequest r : rows) {
			ProviderWorkingHours h = new ProviderWorkingHours();
			h.setClinicId(clinicId);
			h.setProviderId(providerId);
			h.setDayOfWeek(r.dayOfWeek());
			h.setStartTime(r.startTime());
			h.setEndTime(r.endTime());
			h.setLunchStart(r.lunchStart());
			h.setLunchEnd(r.lunchEnd());
			out.add(toResponse(hoursRepository.save(h)));
		}
		return out;
	}

	@Transactional(readOnly = true)
	public List<WorkingHoursResponse> getWorkingHours(UUID providerId) {
		requireProfile(providerId);
		return hoursRepository.findByProviderIdOrderByDayOfWeekAscStartTimeAsc(providerId).stream()
				.map(this::toResponse).toList();
	}

	/**
	 * Package-visible for tests. Blocks on one day must not overlap; lunch must sit
	 * inside its block.
	 */
	public static void validateRows(List<WorkingHoursRequest> rows) {
		for (WorkingHoursRequest r : rows) {
			if (!r.endTime().isAfter(r.startTime())) {
				throw new BusinessException("Working hours on " + r.dayOfWeek() + ": end must be after start");
			}
			if ((r.lunchStart() == null) != (r.lunchEnd() == null)) {
				throw new BusinessException("Lunch needs both a start and an end time");
			}
			if (r.lunchStart() != null) {
				if (!r.lunchEnd().isAfter(r.lunchStart()) || r.lunchStart().isBefore(r.startTime())
						|| r.lunchEnd().isAfter(r.endTime())) {
					throw new BusinessException("Lunch on " + r.dayOfWeek() + " must be inside the working hours");
				}
			}
		}
		Map<DayOfWeek, List<WorkingHoursRequest>> byDay = new EnumMap<>(DayOfWeek.class);
		rows.forEach(r -> byDay.computeIfAbsent(r.dayOfWeek(), d -> new ArrayList<>()).add(r));
		byDay.forEach((day, list) -> {
			list.sort(Comparator.comparing(WorkingHoursRequest::startTime));
			for (int i = 1; i < list.size(); i++) {
				if (list.get(i).startTime().isBefore(list.get(i - 1).endTime())) {
					throw new BusinessException("Working blocks overlap on " + day);
				}
			}
		});
	}

	// ----------------------------------------------------------- leave
	@Transactional
	public LeaveResponse addLeave(UUID providerId, LeaveRequest r) {
		ProviderProfile profile = requireProfile(providerId);
		LocalDate today = LocalDate.now(clock);
		if (r.fromDate().isBefore(today))
			throw new BusinessException("Leave cannot start in the past");
		if (r.toDate().isBefore(r.fromDate()))
			throw new BusinessException("Leave end date is before start date");

		boolean partial = r.fromTime() != null || r.toTime() != null;
		if (partial) {
			if (r.fromTime() == null || r.toTime() == null) {
				throw new BusinessException("Partial-day leave needs both a start and an end time");
			}
			if (!r.fromDate().equals(r.toDate()))
				throw new BusinessException("Partial-day leave must be a single date");
			if (!r.toTime().isAfter(r.fromTime()))
				throw new BusinessException("Leave end time must be after start time");
		}
		long conflicts = partial
				? appointmentRepository.countProviderOverlap(providerId, r.fromDate(), r.fromTime(), r.toTime(),
						AppointmentStatus.BLOCKING)
				: appointmentRepository.countProviderBetween(providerId, r.fromDate(), r.toDate(),
						AppointmentStatus.BLOCKING);
		if (conflicts > 0) {
			throw new BusinessException(
					conflicts + " existing appointment(s) fall in this leave. " + "Reschedule or cancel them first.");
		}
		ProviderLeave l = new ProviderLeave();
		l.setClinicId(profile.getClinicId());
		l.setProviderId(providerId);
		l.setFromDate(r.fromDate());
		l.setToDate(r.toDate());
		l.setFromTime(r.fromTime());
		l.setToTime(r.toTime());
		l.setLeaveType(r.leaveType() == null ? LeaveType.OTHER : r.leaveType());
		l.setReason(r.reason());
		return toResponse(leaveRepository.save(l));
	}

	@Transactional(readOnly = true)
	public List<LeaveResponse> listLeaves(UUID providerId) {
		requireProfile(providerId);
		return leaveRepository
				.findByProviderIdAndToDateGreaterThanEqualOrderByFromDateAsc(providerId, LocalDate.now(clock)).stream()
				.map(this::toResponse).toList();
	}

	@Transactional
	public void deleteLeave(UUID providerId, UUID leaveId) {
		requireProfile(providerId);
		leaveRepository.findByIdAndProviderId(leaveId, providerId)
				.orElseThrow(() -> new ResourceNotFoundException("Leave", leaveId))
				.softDelete(SecurityUtils.currentUsername());
	}

	// ------------------------------------------------- scheduling input
	@Transactional(readOnly = true)
	public DayContext dayContext(UUID providerId, LocalDate date) {
		ProviderProfile p = requireProfile(providerId);
		if (!p.isActive())
			throw new BusinessException("This provider is not accepting appointments");
		List<Row> rows = hoursRepository
				.findByProviderIdAndDayOfWeekOrderByStartTimeAsc(providerId, date.getDayOfWeek()).stream()
				.map(h -> new Row(h.getStartTime(), h.getEndTime(),
						h.getLunchStart() == null ? null : new Interval(h.getLunchStart(), h.getLunchEnd())))
				.toList();
		List<Interval> leaves = leaveRepository.findCovering(providerId, date).stream()
				.map(l -> l.getFromTime() == null ? new Interval(LocalTime.MIN, LocalTime.MAX)
						: new Interval(l.getFromTime(), l.getToTime()))
				.toList();
		return new DayContext(p, rows, leaves);
	}

	// ---------------------------------------------------------- helpers
	private ProviderProfile requireProfile(UUID providerId) {
		ProviderProfile p = profileRepository.findByProviderId(providerId)
				.orElseThrow(() -> new BusinessException("This provider's schedule is not configured yet"));
		ClinicScope.assertSameClinic(p.getClinicId(), "Provider", providerId);
		return p;
	}

	private ProviderProfile ensureProfile(UUID providerId) {
		StaffRef staff = staffClient.requireBookable(providerId);
		ProviderProfile p = profileRepository.findByProviderId(providerId).orElseGet(() -> {
			ClinicSchedulingSettings d = settingsService.effective(ClinicScope.currentClinicId());
			ProviderProfile n = new ProviderProfile();
			n.setProviderId(providerId);
			n.setClinicId(staff.clinicId());
			n.setSlotMinutes(d.getDefaultSlotMinutes());
			n.setMaxParallel(d.getDefaultMaxParallel()); // default 1
			return n;
		});
		p.setProviderName(staff.displayName());
		return profileRepository.save(p);
	}

	private ProviderSettingsResponse toResponse(ProviderProfile p) {
		return new ProviderSettingsResponse(p.getProviderId(), p.getProviderName(), p.getSlotMinutes(),
				p.getMaxParallel(), p.isActive());
	}

	private WorkingHoursResponse toResponse(ProviderWorkingHours h) {
		return new WorkingHoursResponse(h.getId(), h.getDayOfWeek(), h.getStartTime(), h.getEndTime(),
				h.getLunchStart(), h.getLunchEnd());
	}

	private LeaveResponse toResponse(ProviderLeave l) {
		return new LeaveResponse(l.getId(), l.getFromDate(), l.getToDate(), l.getFromTime(), l.getToTime(),
				l.getLeaveType(), l.getReason());
	}
}
