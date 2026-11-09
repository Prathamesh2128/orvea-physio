package com.hms.notification.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record TemplateRequest(@NotBlank @Size(max = 1000) String body, @Size(max = 100) String cloudTemplateName,
		@Size(max = 10) String cloudLanguageCode, @Size(max = 200) String cloudParamOrder, Boolean active) {
}
