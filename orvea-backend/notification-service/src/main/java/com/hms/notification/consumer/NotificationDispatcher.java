package com.hms.notification.consumer;

import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.hms.notification.enums.NotificationStatus;
import com.hms.notification.repository.NotificationRepository;
import com.hms.notification.service.NotificationService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Sends PENDING messages (CLOUD_API only; MANUAL_LINK messages never become
 * PENDING).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationDispatcher {

	private final NotificationRepository repository;
	private final NotificationService service;

	@Scheduled(fixedDelayString = "${physio.notification.dispatch-poll-ms:15000}")
	public void dispatchPending() {
		repository.findSendableIds(NotificationStatus.PENDING, 3, PageRequest.of(0, 50)).forEach(id -> {
			try {
				service.sendOne(id);
			} catch (Exception e) {
				log.error("Could not send notification {}", id, e);
			}
		});
	}
}
