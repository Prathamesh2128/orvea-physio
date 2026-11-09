package com.hms.measurement.repository;

import com.hms.measurement.entity.MeasurementSettings;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface SettingsRepository extends JpaRepository<MeasurementSettings, UUID> {
	Optional<MeasurementSettings> findByClinicId(UUID clinicId);
}