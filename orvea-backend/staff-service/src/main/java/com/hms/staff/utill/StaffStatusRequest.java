package com.hms.staff.utill;

import com.hms.staff.enums.StaffStatus;

import jakarta.validation.constraints.NotNull;

public record StaffStatusRequest(@NotNull StaffStatus status) {

}
