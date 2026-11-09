package com.hms.notification.client;

import java.util.UUID;

import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.hms.common.security.SystemIdentity;

@Component
public class PatientConsentClient {

	@JsonIgnoreProperties(ignoreUnknown = true)
	record Envelope(Boolean data) {
	}

	private final RestClient client;

	public PatientConsentClient(@LoadBalanced RestClient.Builder builder) {
		this.client = builder.baseUrl("http://patient-service").build();
	}

	/**
	 * @return false when the patient has not consented or no longer exists. Any
	 *         other failure (service down) throws, so Kafka retries instead of
	 *         silently dropping the message.
	 */
	public boolean hasWhatsAppConsent(UUID patientId, UUID clinicId) {
		if (patientId == null)
			return false;
		try {
			Envelope e = client.get()
					.uri("/api/v1/internal/patients/{id}/consent?type=REMINDER_SMS_WHATSAPP", patientId)
					.headers(h -> SystemIdentity.apply(h, "notification-service", clinicId)).retrieve()
					.body(Envelope.class);
			return e != null && Boolean.TRUE.equals(e.data());
		} catch (HttpClientErrorException.NotFound e) {
			return false;
		}
	}
}
