package com.hms.appointment.dto;

import java.time.LocalDate;
import java.time.LocalTime;

import com.hms.appointment.enums.LeaveType;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record LeaveRequest(@NotNull LocalDate fromDate, @NotNull LocalDate toDate, LocalTime fromTime, LocalTime toTime,
		LeaveType leaveType, @Size(max = 255) String reason) {
}
