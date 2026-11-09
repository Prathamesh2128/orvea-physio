package com.hms.notification.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.hms.notification.entity.MessagingSettings;

public interface MessagingSettingsRepository extends JpaRepository<MessagingSettings, UUID> {
	Optional<MessagingSettings> findByClinicId(UUID clinicId);
}
