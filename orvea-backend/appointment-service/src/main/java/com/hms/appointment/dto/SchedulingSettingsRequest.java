package com.hms.appointment.dto;

import java.util.List;

import com.hms.appointment.enums.ReminderChannel;

public record SchedulingSettingsRequest(Integer defaultSlotMinutes, Integer defaultMaxParallel,
		Integer maxAdvanceBookingDays, Boolean reminderEnabled, List<Integer> reminderOffsetsMinutes,
		List<ReminderChannel> reminderChannels) {
}
