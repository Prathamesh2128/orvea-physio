package com.hms.common.security;

import java.util.UUID;

import org.springframework.http.HttpHeaders;

import com.hms.common.constants.AppConstants;

/**
 * Background work (Kafka consumers, schedulers) has no logged-in user. It calls
 * other services as a read-only "system" identity, scoped to the clinic of the
 * event it handles. Services trust identity headers, so keep them on a private
 * network (Phase 10).
 */
public final class SystemIdentity {
	private SystemIdentity() {
	}

	public static final String USER_ID = "00000000-0000-0000-0000-000000000000";

	public static void apply(HttpHeaders headers, String serviceName, UUID clinicId) {
		headers.set(AppConstants.HEADER_USER_ID, USER_ID);
		headers.set(AppConstants.HEADER_USERNAME, "system:" + serviceName);
		headers.set(AppConstants.HEADER_ROLES, "SYSTEM");
		headers.set(AppConstants.HEADER_PERMISSIONS, "PATIENT_READ");
		if (clinicId != null)
			headers.set(AppConstants.HEADER_CLINIC_ID, clinicId.toString());
	}
}
