package com.hms.appointment.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import com.hms.appointment.service.VisitTypeService;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class VisitTypeSeeder implements ApplicationRunner {
	private final VisitTypeService visitTypeService;

	@Override
	public void run(ApplicationArguments args) {
		visitTypeService.ensureDefaults(null); // clinics get their own copy on first use
	}
}
