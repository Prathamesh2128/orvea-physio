package com.hms.audit.service;

import java.time.LocalDate;
import java.time.ZoneId;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hms.audit.dato.AuditLogResponse;
import com.hms.audit.entity.AuditLog;
import com.hms.audit.repository.AuditLogRepository;
import com.hms.common.api.PageResponse;
import com.hms.common.event.EventJson;
import com.hms.common.security.ClinicScope;

@Service
public class AuditQueryService {

	private final AuditLogRepository repository;
	private final ZoneId zone;

	public AuditQueryService(AuditLogRepository repository, @Value("${physio.timezone:Asia/Kolkata}") String zone) {
		this.repository = repository;
		this.zone = ZoneId.of(zone);
	}

	/** from/to are clinic-local dates; 'to' is inclusive. */
	@Transactional(readOnly = true)
	public PageResponse<AuditLogResponse> search(String entityType, String entityId, String actor, String action,
			String source, LocalDate from, LocalDate to, Pageable pageable) {
		return PageResponse.from(repository.search(ClinicScope.currentClinicId(), blank(entityType), blank(entityId),
				blank(actor), blank(action), blank(source), from == null ? null : from.atStartOfDay(zone).toInstant(),
				to == null ? null : to.plusDays(1).atStartOfDay(zone).toInstant(), pageable), this::toResponse);
	}

	private AuditLogResponse toResponse(AuditLog a) {
		Object details = a.getDetails();
		try {
			if (a.getDetails() != null)
				details = EventJson.read(a.getDetails(), Object.class);
		} catch (RuntimeException ignored) {
			// keep raw text
		}
		return new AuditLogResponse(a.getId(), a.getOccurredAt(), a.getSourceService(), a.getAction(),
				a.getEntityType(), a.getEntityId(), a.getActor(), details);
	}

	private static String blank(String s) {
		return s == null || s.isBlank() ? null : s.trim();
	}
}
