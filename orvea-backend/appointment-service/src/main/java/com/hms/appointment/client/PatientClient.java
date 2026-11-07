package com.hms.appointment.client;

import java.util.UUID;

import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.hms.common.exception.BusinessException;
import com.hms.common.security.ClinicScope;
import com.hms.common.security.IdentityPropagationInterceptor;

@Component
public class PatientClient {

	@JsonIgnoreProperties(ignoreUnknown = true)
	record Envelope(PatientRef data) {
	}

	private final RestClient client;

	public PatientClient(@LoadBalanced RestClient.Builder builder) {
		this.client = builder.baseUrl("http://patient-service").requestInterceptor(new IdentityPropagationInterceptor())
				.build();
	}

	public PatientRef get(UUID id) {
		try {
			Envelope e = client.get().uri("/api/v1/internal/patients/{id}", id).retrieve().body(Envelope.class);
			if (e == null || e.data() == null)
				throw new BusinessException("Patient not found: " + id);
			ClinicScope.assertSameClinic(e.data().clinicId(), "Patient", id);
			return e.data();
		} catch (HttpClientErrorException.NotFound ex) {
			throw new BusinessException("Patient not found: " + id);
		} catch (RestClientException ex) {
			throw new BusinessException("Patient service is unavailable. Try again shortly.",
					HttpStatus.SERVICE_UNAVAILABLE);
		}
	}
}
