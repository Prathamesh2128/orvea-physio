package com.hms.billing.consumer;

import java.time.Clock;
import java.time.LocalDate;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.hms.billing.service.PackageService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class PackageExpiryScheduler {

	private final PackageService packageService;
	private final Clock clock;

	@Scheduled(cron = "0 30 1 * * *", zone = "${physio.timezone:Asia/Kolkata}")
	public void expire() {
		int n = packageService.expireOverdue(LocalDate.now(clock));
		if (n > 0)
			log.info("{} package(s) expired", n);
	}
}