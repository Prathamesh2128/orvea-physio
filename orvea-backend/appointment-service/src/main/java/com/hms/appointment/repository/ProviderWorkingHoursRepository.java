package com.hms.appointment.repository;

import java.time.DayOfWeek;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.hms.appointment.entity.ProviderWorkingHours;

public interface ProviderWorkingHoursRepository extends JpaRepository<ProviderWorkingHours, UUID> {
	List<ProviderWorkingHours> findByProviderIdOrderByDayOfWeekAscStartTimeAsc(UUID providerId);

	List<ProviderWorkingHours> findByProviderIdAndDayOfWeekOrderByStartTimeAsc(UUID providerId, DayOfWeek day);
}
