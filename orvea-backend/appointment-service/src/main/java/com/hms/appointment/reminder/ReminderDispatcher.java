package com.hms.appointment.reminder;

import java.time.Clock;
import java.time.LocalDateTime;

import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.hms.appointment.enums.ReminderStatus;
import com.hms.appointment.repository.ReminderTaskRepository;
import com.hms.appointment.service.ReminderService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReminderDispatcher {

	private final ReminderTaskRepository taskRepository;
	private final ReminderService reminderService;
	private final Clock clock;

	@Scheduled(fixedDelayString = "${physio.reminders.poll-ms:60000}")
	public void dispatchDue() {
		taskRepository.findDueIds(ReminderStatus.PENDING, LocalDateTime.now(clock), PageRequest.of(0, 100))
				.forEach(id -> {
					try {
						reminderService.dispatchOne(id);
					} catch (Exception e) {
						log.error("Could not dispatch reminder {}", id, e);
					}
				});
	}
}
