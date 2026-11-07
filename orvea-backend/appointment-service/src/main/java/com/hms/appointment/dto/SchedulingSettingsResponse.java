package com.hms.appointment.dto;

import java.util.List;

import com.hms.appointment.enums.ReminderChannel;

public record SchedulingSettingsResponse(int defaultSlotMinutes, int defaultMaxParallel, int maxAdvanceBookingDays,
		boolean reminderEnabled, List<Integer> reminderOffsetsMinutes, List<ReminderChannel> reminderChannels) {
}