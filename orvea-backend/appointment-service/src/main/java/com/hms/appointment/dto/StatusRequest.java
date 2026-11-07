package com.hms.appointment.dto;

import com.hms.appointment.enums.AppointmentStatus;

import jakarta.validation.constraints.NotNull;

public record StatusRequest(@NotNull AppointmentStatus status) {}
