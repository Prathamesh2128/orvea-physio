package com.hms.appointment.dto;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import com.hms.appointment.util.SlotCalculator;

public record DayScheduleResponse(UUID providerId, String providerName, LocalDate date, int slotMinutes,
		int maxParallel, List<SlotCalculator.Cell> cells, List<AppointmentResponse> appointments) {
}
