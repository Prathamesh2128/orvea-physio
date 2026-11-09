package com.hms.notification.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.hms.notification.entity.MessageTemplate;
import com.hms.notification.enums.MessageType;

public interface MessageTemplateRepository extends JpaRepository<MessageTemplate, UUID> {
	List<MessageTemplate> findByClinicIdOrderByMessageTypeAsc(UUID clinicId);

	Optional<MessageTemplate> findByClinicIdAndMessageType(UUID clinicId, MessageType type);
}
