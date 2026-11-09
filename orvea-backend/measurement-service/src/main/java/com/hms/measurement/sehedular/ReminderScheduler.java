package com.hms.measurement.sehedular;

import java.time.Clock;
import java.time.LocalDate;
import java.util.UUID;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.hms.measurement.service.MeasurementReminderService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReminderScheduler {

	private final MeasurementReminderService service;
	private final Clock clock;

	/**
	 * Hourly 08:00-20:00 clinic time, so a restart never causes a night-time
	 * message; repeats are prevented by a marker.
	 */
	@Scheduled(cron = "${physio.measurement.reminder-cron:0 0 8-20 * * *}", zone = "${physio.timezone:Asia/Kolkata}")
	public void run() {
		LocalDate today = LocalDate.now(clock);
		for (UUID id : service.candidateIds(today)) {
			try {
				service.remindOne(id, today);
			} catch (Exception e) {
				log.error("Measurement reminder for plan {} failed: {}", id, e.getMessage());
			}
		}
	}
}