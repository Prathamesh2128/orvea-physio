package com.hms.measurement;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableScheduling;

import com.hms.common.config.ServiceSecurityConfig;

@SpringBootApplication
@EnableJpaAuditing(auditorAwareRef = "auditorAware")
@EnableScheduling
@Import(ServiceSecurityConfig.class)
public class MeasurementServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(MeasurementServiceApplication.class, args);
	}

}
