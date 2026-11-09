package com.hms.measurement.client;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.hms.common.security.IdentityPropagationInterceptor;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class AppointmentClient {

	@JsonIgnoreProperties(ignoreUnknown = true)
	public record NextVisit(UUID patientId, UUID appointmentId, UUID providerId, String providerName, LocalDate date,
			LocalTime startTime) {
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	record Envelope(List<NextVisit> data) {
	}

	private final RestClient client;

	public AppointmentClient(@LoadBalanced RestClient.Builder builder) {
		this.client = builder.baseUrl("http://appointment-service")
				.requestInterceptor(new IdentityPropagationInterceptor()).build();
	}

	/**
	 * Visit times are a convenience on the due list: if appointment-service is down
	 * the list still works.
	 */
	public Map<UUID, NextVisit> nextVisits(Collection<UUID> patientIds) {
		if (patientIds == null || patientIds.isEmpty())
			return Map.of();
		try {
			String ids = patientIds.stream().map(UUID::toString).collect(Collectors.joining(","));
			Envelope e = client.get().uri("/api/v1/internal/appointments/next?patientIds={ids}", ids).retrieve()
					.body(Envelope.class);
			Map<UUID, NextVisit> out = new HashMap<>();
			if (e != null && e.data() != null)
				e.data().forEach(v -> out.put(v.patientId(), v));
			return out;
		} catch (RuntimeException ex) {
			log.warn("Could not load next visits: {}", ex.getMessage());
			return Map.of();
		}
	}
}