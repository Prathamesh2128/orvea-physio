package com.hms.appointment.service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hms.appointment.dto.ReminderResponse;
import com.hms.appointment.entity.Appointment;
import com.hms.appointment.entity.ClinicSchedulingSettings;
import com.hms.appointment.entity.ReminderTask;
import com.hms.appointment.enums.AppointmentStatus;
import com.hms.appointment.enums.ReminderChannel;
import com.hms.appointment.enums.ReminderStatus;
import com.hms.appointment.reminder.ReminderMessage;
import com.hms.appointment.reminder.ReminderPublisher;
import com.hms.appointment.repository.AppointmentRepository;
import com.hms.appointment.repository.ReminderTaskRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReminderService {

	private final ReminderTaskRepository taskRepository;
	private final AppointmentRepository appointmentRepository;
	private final SchedulingSettingsService settingsService;
	private final ReminderPublisher publisher;
	private final Clock clock;

	/**
	 * One task per configured offset; offsets that are already in the past are
	 * skipped.
	 */
	@Transactional
	public void scheduleFor(Appointment a) {
		ClinicSchedulingSettings s = settingsService.effective(a.getClinicId());
		if (!s.isReminderEnabled() || s.getReminderChannels().isEmpty())
			return;

		LocalDateTime start = LocalDateTime.of(a.getAppointmentDate(), a.getStartTime());
		LocalDateTime now = LocalDateTime.now(clock);
		String channels = s.getReminderChannels().stream().map(Enum::name).collect(Collectors.joining(","));

		for (Integer offset : s.getReminderOffsetsMinutes()) {
			LocalDateTime remindAt = start.minusMinutes(offset);
			if (!remindAt.isAfter(now))
				continue;
			ReminderTask t = new ReminderTask();
			t.setClinicId(a.getClinicId());
			t.setAppointmentId(a.getId());
			t.setRemindAt(remindAt);
			t.setOffsetMinutes(offset);
			t.setChannels(channels);
			taskRepository.save(t);
		}
	}

	@Transactional
	public void cancelPending(UUID appointmentId) {
		taskRepository.cancelPending(appointmentId, ReminderStatus.PENDING, ReminderStatus.CANCELLED);
	}

	@Transactional(readOnly = true)
	public List<ReminderResponse> list(UUID appointmentId) {
		return taskRepository.findByAppointmentIdOrderByRemindAtAsc(appointmentId).stream()
				.map(t -> new ReminderResponse(t.getId(), t.getRemindAt(), t.getOffsetMinutes(), t.getChannels(),
						t.getStatus(), t.getDispatchedAt(), t.getLastError()))
				.toList();
	}

	/**
	 * Called by the scheduler for each due task. The atomic claim prevents
	 * duplicate sends across nodes.
	 */
	@Transactional
	public void dispatchOne(UUID taskId) {
		LocalDateTime now = LocalDateTime.now(clock);
		if (taskRepository.claim(taskId, ReminderStatus.PENDING, ReminderStatus.DISPATCHED, now) == 0)
			return;

		ReminderTask task = taskRepository.findById(taskId).orElseThrow();
		Appointment a = appointmentRepository.findById(task.getAppointmentId()).orElse(null);
		boolean remindable = a != null
				&& (a.getStatus() == AppointmentStatus.BOOKED || a.getStatus() == AppointmentStatus.CONFIRMED)
				&& LocalDateTime.of(a.getAppointmentDate(), a.getStartTime()).isAfter(now);
		if (!remindable) {
			task.setStatus(ReminderStatus.SKIPPED);
			return;
		}
		ReminderMessage msg = new ReminderMessage(task.getId(), a.getId(), a.getClinicId(), a.getPatientId(),
				a.getPatientName(), a.getPatientPhone(), a.getProviderName(), a.getVisitType().getName(),
				LocalDateTime.of(a.getAppointmentDate(), a.getStartTime()),
				Arrays.stream(task.getChannels().split(",")).map(ReminderChannel::valueOf).toList(),
				task.getOffsetMinutes());
		try {
			publisher.publish(msg);
		} catch (Exception e) {
			log.error("Reminder {} failed", taskId, e);
			task.setStatus(ReminderStatus.FAILED);
			String m = String.valueOf(e.getMessage());
			task.setLastError(m.substring(0, Math.min(255, m.length())));
		}
	}
}
