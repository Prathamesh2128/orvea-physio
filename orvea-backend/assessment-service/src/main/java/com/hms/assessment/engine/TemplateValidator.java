package com.hms.assessment.engine;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import com.hms.assessment.engine.TemplateDefinition.CaptureHint;
import com.hms.assessment.engine.TemplateDefinition.Option;
import com.hms.assessment.engine.TemplateDefinition.Question;
import com.hms.assessment.engine.TemplateDefinition.Section;
import com.hms.assessment.engine.TemplateDefinition.ShowIf;
import com.hms.assessment.util.Json;
import com.hms.common.exception.BusinessException;

public final class TemplateValidator {
	private TemplateValidator() {
	}

	private static final Pattern KEY = Pattern.compile("^[a-z][a-z0-9_]{0,39}$");
	private static final Pattern ANGLE_KEY = Pattern.compile("^[a-z][a-z0-9_]{0,59}$");
	private static final Pattern OPTION = Pattern.compile("^[A-Za-z0-9_.\\-]{1,40}$");
	private static final Set<QuestionType> CONDITIONABLE = EnumSet.of(QuestionType.SINGLE_CHOICE,
			QuestionType.MULTI_CHOICE, QuestionType.YES_NO, QuestionType.SCALE, QuestionType.NUMBER);

	/**
	 * Strict parse (unknown fields are an error, so typos are caught) and
	 * validation.
	 */
	public static TemplateDefinition parseAndValidate(String json) {
		TemplateDefinition d;
		try {
			d = Json.readStrict(json, TemplateDefinition.class);
		} catch (RuntimeException e) {
			String m = String.valueOf(e.getMessage());
			throw new BusinessException("Template JSON is invalid: "
					+ m.split("\n")[0].substring(0, Math.min(200, m.split("\n")[0].length())));
		}
		validate(d);
		return d;
	}

	public static int countQuestions(TemplateDefinition d) {
		return d.sections().stream().mapToInt(s -> s.questions().size()).sum();
	}

	public static void validate(TemplateDefinition d) {
		if (d == null || d.sections() == null || d.sections().isEmpty())
			fail("at least one section is required");
		if (d.sections().size() > 20)
			fail("at most 20 sections");
		Set<String> sectionKeys = new HashSet<>();
		Map<String, QuestionType> earlier = new LinkedHashMap<>();
		for (Section s : d.sections()) {
			if (s == null || s.key() == null || !KEY.matcher(s.key()).matches()) {
				fail("section key must be lowercase letters, digits or underscore, starting with a letter");
			}
			if (!sectionKeys.add(s.key()))
				fail("duplicate section key '" + s.key() + "'");
			if (blank(s.title()))
				fail("section '" + s.key() + "' needs a title");
			if (s.questions() == null || s.questions().isEmpty())
				fail("section '" + s.key() + "' has no questions");
			if (s.questions().size() > 60)
				fail("section '" + s.key() + "' has more than 60 questions");
			for (Question q : s.questions()) {
				validateQuestion(q, earlier);
				earlier.put(q.key(), q.type());
			}
		}
		if (earlier.size() > 200)
			fail("at most 200 questions per template");
	}

	private static void validateQuestion(Question q, Map<String, QuestionType> earlier) {
		if (q == null || q.key() == null || !KEY.matcher(q.key()).matches()) {
			fail("question key must be lowercase letters, digits or underscore, starting with a letter");
		}
		if (earlier.containsKey(q.key()))
			fail("duplicate question key '" + q.key() + "' (keys must be unique in a template)");
		if (q.type() == null)
			fail("question '" + q.key() + "' needs a type");
		if (blank(q.label()) || q.label().length() > 200)
			fail("question '" + q.key() + "' needs a label of up to 200 characters");

		boolean choice = q.type() == QuestionType.SINGLE_CHOICE || q.type() == QuestionType.MULTI_CHOICE;
		if (choice) {
			if (q.options() == null || q.options().size() < 2)
				fail("question '" + q.key() + "' needs at least 2 options");
			Set<String> values = new HashSet<>();
			for (Option o : q.options()) {
				if (o == null || o.value() == null || !OPTION.matcher(o.value()).matches()) {
					fail("question '" + q.key() + "' has an option value with unsupported characters");
				}
				if (!values.add(o.value()))
					fail("question '" + q.key() + "' repeats option '" + o.value() + "'");
				if (blank(o.label()))
					fail("question '" + q.key() + "' has an option without a label");
			}
		} else if (q.options() != null && !q.options().isEmpty()) {
			fail("question '" + q.key() + "': options are only for choice questions");
		}
		if ((q.type() == QuestionType.SCALE || q.type() == QuestionType.NUMBER || q.type() == QuestionType.ANGLE)
				&& q.min() != null && q.max() != null && q.min() >= q.max()) {
			fail("question '" + q.key() + "': min must be lower than max");
		}
		if (q.type() == QuestionType.TEXT && q.maxLength() != null && (q.maxLength() < 1 || q.maxLength() > 2000)) {
			fail("question '" + q.key() + "': maxLength must be 1 to 2000");
		}
		if (q.showIf() != null) {
			ShowIf s = q.showIf();
			QuestionType parent = s.question() == null ? null : earlier.get(s.question());
			if (parent == null)
				fail("question '" + q.key() + "': showIf must point to an earlier question");
			if (!CONDITIONABLE.contains(parent))
				fail("question '" + q.key() + "': showIf cannot depend on a " + parent + " question");
			boolean hasValue = s.value() != null;
			boolean hasAny = s.anyOf() != null && !s.anyOf().isEmpty();
			if (hasValue == hasAny)
				fail("question '" + q.key() + "': showIf needs exactly one of value or anyOf");
		}
		if (q.capture() != null) {
			if (q.type() != QuestionType.ANGLE)
				fail("question '" + q.key() + "': capture is only for ANGLE questions");
			CaptureHint c = q.capture();
			if (!"ROM".equals(c.kind()) && !"POSTURE".equals(c.kind()))
				fail("question '" + q.key() + "': capture kind must be ROM or POSTURE");
			if (c.angleKey() == null || !ANGLE_KEY.matcher(c.angleKey()).matches())
				fail("question '" + q.key() + "': invalid capture angleKey");
		}
	}

	private static boolean blank(String s) {
		return s == null || s.isBlank();
	}

	private static void fail(String message) {
		throw new BusinessException("Template error: " + message);
	}
}