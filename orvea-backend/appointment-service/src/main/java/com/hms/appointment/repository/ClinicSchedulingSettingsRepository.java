package com.hms.appointment.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.hms.appointment.entity.ClinicSchedulingSettings;

public interface ClinicSchedulingSettingsRepository extends JpaRepository<ClinicSchedulingSettings, UUID> {
	Optional<ClinicSchedulingSettings> findByClinicId(UUID clinicId);
}