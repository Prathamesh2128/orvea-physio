package com.hms.billing.repository;

import com.hms.billing.entity.BillingSettings;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface BillingSettingsRepository extends JpaRepository<BillingSettings, UUID> {
	Optional<BillingSettings> findByClinicId(UUID clinicId);
}