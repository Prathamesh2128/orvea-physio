package com.hms.audit.dato;

import java.time.Instant;
import java.util.UUID;

public record AuditLogResponse(UUID id, Instant occurredAt, String sourceService, String action, String entityType,
		String entityId, String actor, Object details) {
}
