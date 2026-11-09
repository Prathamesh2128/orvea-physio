package com.hms.assessment.engine;

import java.util.List;

public record TemplateDefinition(List<Section> sections) {

	public record Section(String key, String title, String description, List<Question> questions) {
	}

	/**
	 * required: must be answered before Complete (not before saving a draft).
	 * carryForward: copy from the last visit? null = by type (measurements are
	 * never copied by default). showIf: shown only when an EARLIER question has the
	 * given value (or one of anyOf). capture: tells the app which camera
	 * measurement belongs to an ANGLE question.
	 */
	public record Question(String key, QuestionType type, String label, String help, Boolean required,
			Boolean carryForward, String unit, Double min, Double max, Integer maxLength, List<Option> options,
			ShowIf showIf, CaptureHint capture) {
	}

	public record Option(String value, String label) {
	}

	public record ShowIf(String question, Object value, List<Object> anyOf) {
	}

	public record CaptureHint(String kind, String angleKey, String region, String side) {
	}
}