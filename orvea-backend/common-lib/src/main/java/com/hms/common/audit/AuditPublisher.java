package com.hms.common.audit;

import java.util.Map;
import java.util.UUID;

import com.hms.common.security.ClinicScope;
import com.hms.common.security.SecurityUtils;

public interface AuditPublisher {

	/**
	 * Records who did what to which record. Keep details free of personal data (use
	 * ids / UHID).
	 */
	void recordAs(String actor, UUID clinicId, String action, String entityType, Object entityId,
			Map<String, Object> details);

	/** Uses the logged-in user and their clinic. */
	default void record(String action, String entityType, Object entityId, Map<String, Object> details) {
		recordAs(SecurityUtils.currentUsername(), ClinicScope.currentClinicId(), action, entityType, entityId, details);
	}
}