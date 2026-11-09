package com.hms.measurement.config;

import java.time.Clock;
import java.time.ZoneId;

import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.beans.factory.annotation.Value;

@Configurable
public class ClockConfig {
	Clock clock(@Value("${physio.timezone:Asia/Kolkata}") String zone) {
		return Clock.system(ZoneId.of(zone));
	}
}
