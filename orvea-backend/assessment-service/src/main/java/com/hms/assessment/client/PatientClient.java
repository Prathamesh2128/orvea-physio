package com.hms.assessment.client;

import java.util.UUID;
import java.util.function.Supplier;

import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.core.ParameterizedTypeReference;
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
	record Envelope<T>(T data) {
	}

	private final RestClient client;

	public PatientClient(@LoadBalanced RestClient.Builder builder) {
		this.client = builder.baseUrl("http://patient-service").requestInterceptor(new IdentityPropagationInterceptor())
				.build();
	}

	public PatientRef getPatient(UUID patientId) {
		PatientRef p = call("Patient", patientId, () -> client.get().uri("/api/v1/internal/patients/{id}", patientId)
				.retrieve().body(new ParameterizedTypeReference<Envelope<PatientRef>>() {
				}));
		ClinicScope.assertSameClinic(p.clinicId(), "Patient", patientId);
		return p;
	}

	public CaseRef getCase(UUID patientId, UUID caseId) {
		return call("Treatment case", caseId,
				() -> client.get().uri("/api/v1/internal/patients/{pid}/cases/{cid}", patientId, caseId).retrieve()
						.body(new ParameterizedTypeReference<Envelope<CaseRef>>() {
						}));
	}

	/**
	 * @param type a patient-service ConsentType name, e.g. CAMERA_POSE_ANALYSIS,
	 *             PHOTO_VIDEO
	 */
	public boolean hasConsent(UUID patientId, String type) {
		Boolean ok = call("Patient", patientId,
				() -> client.get().uri("/api/v1/internal/patients/{id}/consent?type={type}", patientId, type).retrieve()
						.body(new ParameterizedTypeReference<Envelope<Boolean>>() {
						}));
		return Boolean.TRUE.equals(ok);
	}

	private <T> T call(String what, UUID id, Supplier<Envelope<T>> request) {
		try {
			Envelope<T> e = request.get();
			if (e == null || e.data() == null)
				throw new BusinessException(what + " not found: " + id);
			return e.data();
		} catch (HttpClientErrorException.NotFound ex) {
			throw new BusinessException(what + " not found: " + id);
		} catch (RestClientException ex) {
			throw new BusinessException("Patient service is unavailable. Try again shortly.",
					HttpStatus.SERVICE_UNAVAILABLE);
		}
	}
}