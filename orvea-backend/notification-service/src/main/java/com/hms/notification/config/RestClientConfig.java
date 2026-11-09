package com.hms.notification.config;

import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Scope;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfig {

	/** Default builder: Eureka's own transport and the Meta Graph API client. */
	@Bean
	@Primary
	@Scope("prototype")
	RestClient.Builder plainRestClientBuilder() {
		return RestClient.builder();
	}

	/** Only where @LoadBalanced is requested (PatientConsentClient). */
	@Bean
	@LoadBalanced
	@Scope("prototype")
	RestClient.Builder loadBalancedRestClientBuilder() {
		return RestClient.builder();
	}
}
