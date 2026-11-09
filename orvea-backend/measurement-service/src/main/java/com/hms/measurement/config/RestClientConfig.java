package com.hms.measurement.config;

import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Scope;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfig {

	/** Default builder (also used by Eureka's own transport). */
	@Bean
	@Primary
	@Scope("prototype")
	RestClient.Builder plainRestClientBuilder() {
		return RestClient.builder();
	}

	/** Only where @LoadBalanced is requested (PatientClient). */
	@Bean
	@LoadBalanced
	@Scope("prototype")
	RestClient.Builder loadBalancedRestClientBuilder() {
		return RestClient.builder();
	}
}
