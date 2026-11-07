package com.hms.patient.config;

import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Scope;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfig {

	/**
	 * Default builder for everything else, including the Eureka client's own
	 * transport.
	 */
	@Bean
	@Primary
	@Scope("prototype")
	RestClient.Builder plainRestClientBuilder() {
		return RestClient.builder();
	}

	/**
	 * Injected only where @LoadBalanced is requested (StaffClient, PatientClient).
	 */
	@Bean
	@LoadBalanced
	@Scope("prototype")
	RestClient.Builder loadBalancedRestClientBuilder() {
		return RestClient.builder();
	}
}
