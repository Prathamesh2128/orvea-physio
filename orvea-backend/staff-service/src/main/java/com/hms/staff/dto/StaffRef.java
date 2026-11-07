package com.hms.staff.dto;

import java.util.UUID;

public record StaffRef(UUID id, String displayName, String staffType, boolean active, boolean bookable, UUID clinicId) {
}
