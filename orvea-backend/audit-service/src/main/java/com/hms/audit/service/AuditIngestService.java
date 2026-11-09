package com.hms.audit.service;

import java.time.Clock;
import java.time.Instant;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hms.audit.entity.AuditLog;
import com.hms.audit.repository.AuditLogRepository;
import com.hms.common.event.BaseEvent;
import com.hms.common.event.EventJson;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuditIngestService {

	private final AuditLogRepository repository;
	private final Clock clock;

	/** @return false when the event was already stored (Kafka redelivery). */
	@Transactional
	public boolean ingest(BaseEvent e) {
		if (e.getEventId() == null || e.getEventType() == null)
			return false;
		if (repository.existsByEventId(e.getEventId()))
			return false;

		AuditLog log = new AuditLog();
		log.setEventId(e.getEventId());
		log.setClinicId(e.getClinicId());
		log.setSourceService(e.getSourceService());
		log.setAction(e.getEventType());
		log.setEntityType(e.text("entityType") == null ? "UNKNOWN" : e.text("entityType"));
		log.setEntityId(e.text("entityId"));
		String actor = e.text("actor") != null ? e.text("actor") : e.getPerformedBy();
		log.setActor(actor == null || actor.isBlank() ? "SYSTEM" : actor);
		log.setOccurredAt(e.getOccurredAt() == null ? Instant.now(clock) : e.getOccurredAt());
		Object details = e.getPayload() == null ? null : e.getPayload().get("details");
		log.setDetails(details == null ? null : EventJson.write(details));
		log.setReceivedAt(Instant.now(clock));
		repository.save(log);
		return true;
	}
}
