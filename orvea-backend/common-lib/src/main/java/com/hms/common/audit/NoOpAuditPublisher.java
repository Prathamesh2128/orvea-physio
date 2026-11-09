package com.hms.common.audit;

import java.util.Map;
import java.util.UUID;

public class NoOpAuditPublisher implements AuditPublisher {
	@Override
	public void recordAs(String actor, UUID clinicId, String action, String entityType, Object entityId,
			Map<String, Object> details) {
		// auditing switched off (physio.audit.enabled=false) or Kafka not available
	}
}
