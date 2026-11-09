package com.hms.notification.config;

import java.time.Clock;
import java.time.ZoneId;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ClockConfig {
	@Bean
	Clock clock(@Value("${physio.timezone:Asia/Kolkata}") String zone) {
		return Clock.system(ZoneId.of(zone));
	}
}
