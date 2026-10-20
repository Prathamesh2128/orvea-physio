package com.hms.common.security;

import java.util.UUID;

import com.hms.common.exception.ResourceNotFoundException;

public final class ClinicScope {
	private ClinicScope() {
	}

	public static UUID currentClinicId() {
		return SecurityUtils.currentUser().map(AuthenticatedUser::clinicId).orElse(null);
	}

	/** Hides records of other clinics by answering 404 instead of 403. */
	public static void assertSameClinic(UUID entityClinicId, String resource, Object id) {
		UUID caller = currentClinicId();
		if (caller != null && !caller.equals(entityClinicId)) {
			throw new ResourceNotFoundException(resource, id);
		}
	}
}
