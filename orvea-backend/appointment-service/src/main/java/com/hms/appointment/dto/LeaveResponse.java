package com.hms.appointment.dto;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

import com.hms.appointment.enums.LeaveType;

public record LeaveResponse(UUID id, LocalDate fromDate, LocalDate toDate, LocalTime fromTime, LocalTime toTime,
		LeaveType leaveType, String reason) {
}
