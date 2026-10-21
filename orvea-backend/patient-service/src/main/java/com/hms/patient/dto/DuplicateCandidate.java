package com.hms.patient.dto;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record DuplicateCandidate(UUID id, String uhid, String fullName, LocalDate dateOfBirth, String phone,
		List<String> matchReasons) {

}
