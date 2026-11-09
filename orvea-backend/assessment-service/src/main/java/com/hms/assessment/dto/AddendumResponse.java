package com.hms.assessment.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record AddendumResponse(UUID id, String text, String author, LocalDateTime createdAt) {
}