package com.hms.patient.client;

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
import com.hms.patient.dto.StaffRef;

@Component
public class StaffClient {

	@JsonIgnoreProperties(ignoreUnknown = true)
	record Envelope(StaffRef data) {
	}

	private final RestClient client;

	public StaffClient(@LoadBalanced RestClient.Builder builder) {
		this.client = builder.baseUrl("http://staff-service").requestInterceptor(new IdentityPropagationInterceptor())
				.build();
	}

	public StaffRef get(UUID id) {
		try {
			Envelope e = client.get().uri("/api/v1/internal/staff/{id}", id).retrieve().body(Envelope.class);
			if (e == null || e.data() == null)
				throw new BusinessException("Staff member not found: " + id);
			return e.data();
		} catch (HttpClientErrorException.NotFound ex) {
			throw new BusinessException("Staff member not found: " + id);
		} catch (RestClientException ex) {
			throw new BusinessException("Staff service is unavailable. Try again shortly.",
					HttpStatus.SERVICE_UNAVAILABLE);
		}
	}

	/**
	 * Must exist in the caller's clinic, be active and be a treating role
	 * (physiotherapist / doctor).
	 */
	public StaffRef requireBookable(UUID id) {
		StaffRef s = get(id);
		ClinicScope.assertSameClinic(s.clinicId(), "Staff member", id);
		if (!s.active() || !s.bookable()) {
			throw new BusinessException(s.displayName() + " is not an active treating therapist or doctor");
		}
		return s;
	}
}
