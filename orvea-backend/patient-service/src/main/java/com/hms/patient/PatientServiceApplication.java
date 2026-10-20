package com.hms.patient;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

import com.hms.common.config.ServiceSecurityConfig;

@SpringBootApplication
@EnableJpaAuditing(auditorAwareRef = "auditorAware")
@Import(ServiceSecurityConfig.class)
public class PatientServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(PatientServiceApplication.class, args);
	}

}
