package com.hms.notification.dto;

import java.util.UUID;

import com.hms.notification.enums.MessageType;

public record TemplateResponse(UUID id, MessageType messageType, String body, String cloudTemplateName,
		String cloudLanguageCode, String cloudParamOrder, boolean active) {
}
