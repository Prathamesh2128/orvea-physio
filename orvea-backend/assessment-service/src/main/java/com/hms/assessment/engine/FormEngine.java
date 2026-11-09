package com.hms.assessment.engine;

import com.hms.assessment.engine.Snapshot.SnapshotTemplate;
import com.hms.assessment.engine.TemplateDefinition.Option;
import com.hms.assessment.engine.TemplateDefinition.Question;
import com.hms.assessment.engine.TemplateDefinition.Section;
import com.hms.assessment.engine.TemplateDefinition.ShowIf;
import com.hms.common.enums.BodyRegion;
import com.hms.common.enums.Laterality;
import com.hms.common.exception.BusinessException;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.function.BiConsumer;

/** Pure form logic (no database): visibility, answer checking, progress. */
public final class FormEngine {
	private FormEngine() {
	}

	public static final String METHOD_MANUAL = "MANUAL";
	public static final String METHOD_CAMERA = "CAMERA_CONFIRMED";

	public record Progress(int answered, int total, int requiredAnswered, int requiredTotal, int percent,
			boolean canComplete) {
	}

	// ---------------------------------------------------- visibility
	public static boolean isVisible(Question q, Map<String, Object> answers) {
		ShowIf s = q.showIf();
		if (s == null)
			return true;
		Object actual = answers == null ? null : answers.get(s.question());
		if (s.value() != null)
			return matches(actual, s.value());
		if (s.anyOf() != null)
			return s.anyOf().stream().anyMatch(v -> matches(actual, v));
		return true;
	}

	private static boolean matches(Object actual, Object expected) {
		if (actual == null)
			return false;
		if (actual instanceof List<?> list)
			return list.stream().anyMatch(x -> scalarEquals(x, expected));
		return scalarEquals(actual, expected);
	}

	private static boolean scalarEquals(Object a, Object b) {
		if (a instanceof Number x && b instanceof Number y)
			return Math.abs(x.doubleValue() - y.doubleValue()) < 1e-9;
		return Objects.equals(a, b);
	}

	/** Numeric-tolerant deep equality (JSON 112 and 112.0 are the same answer). */
	public static boolean sameValue(Object a, Object b) {
		if (a instanceof Map<?, ?> x && b instanceof Map<?, ?> y) {
			if (!x.keySet().equals(y.keySet()))
				return false;
			for (Object k : x.keySet())
				if (!sameValue(x.get(k), y.get(k)))
					return false;
			return true;
		}
		if (a instanceof List<?> x && b instanceof List<?> y) {
			if (x.size() != y.size())
				return false;
			for (int i = 0; i < x.size(); i++)
				if (!sameValue(x.get(i), y.get(i)))
					return false;
			return true;
		}
		return scalarEquals(a, b);
	}

	// ------------------------------------------------------ answers
	public static boolean isAnswered(Question q, Object value) {
		if (value == null)
			return false;
		if (value instanceof String s)
			return !s.isBlank();
		if (value instanceof Collection<?> c)
			return !c.isEmpty();
		return true;
	}

	/** Measurements must be re-taken each visit; stable facts can be copied. */
	public static boolean carryForwardable(Question q) {
		if (q.carryForward() != null)
			return q.carryForward();
		return switch (q.type()) {
		case SINGLE_CHOICE, MULTI_CHOICE, YES_NO, TEXT, DATE, BODY_MAP -> true;
		case SCALE, NUMBER, ANGLE -> false;
		};
	}

	/**
	 * @return the cleaned answer (null clears it). Throws BusinessException if it
	 *         does not fit the question.
	 */
	public static Object validate(Question q, Object value, boolean allowCameraMethod) {
		if (value == null)
			return null;
		return switch (q.type()) {
		case SINGLE_CHOICE -> singleChoice(q, value);
		case MULTI_CHOICE -> multiChoice(q, value);
		case YES_NO -> yesNo(q, value);
		case SCALE -> scale(q, value);
		case NUMBER -> number(q, value);
		case TEXT -> text(q, value);
		case DATE -> date(q, value);
		case BODY_MAP -> bodyMap(q, value);
		case ANGLE -> angle(q, value, allowCameraMethod);
		};
	}

	private static BusinessException bad(Question q, String why) {
		return new BusinessException("Invalid answer for '" + q.label() + "': " + why);
	}

	private static Set<String> optionValues(Question q) {
		Set<String> v = new HashSet<>();
		if (q.options() != null)
			for (Option o : q.options())
				v.add(o.value());
		return v;
	}

	private static Object singleChoice(Question q, Object v) {
		if (v instanceof String s && optionValues(q).contains(s))
			return s;
		throw bad(q, "not one of the allowed choices");
	}

	private static Object multiChoice(Question q, Object v) {
		if (!(v instanceof List<?> list))
			throw bad(q, "expected a list of choices");
		Set<String> allowed = optionValues(q);
		Set<String> out = new LinkedHashSet<>();
		for (Object o : list) {
			if (!(o instanceof String s) || !allowed.contains(s))
				throw bad(q, "contains a choice that is not allowed");
			out.add(s);
		}
		return new ArrayList<>(out);
	}

	private static Object yesNo(Question q, Object v) {
		if (v instanceof Boolean)
			return v;
		throw bad(q, "must be yes or no");
	}

	private static Object scale(Question q, Object v) {
		if (!(v instanceof Number n) || n.doubleValue() != Math.rint(n.doubleValue()))
			throw bad(q, "must be a whole number");
		double min = q.min() == null ? 0 : q.min();
		double max = q.max() == null ? 10 : q.max();
		if (n.doubleValue() < min || n.doubleValue() > max) {
			throw bad(q, "must be between " + (int) min + " and " + (int) max);
		}
		return n.intValue();
	}

	private static Object number(Question q, Object v) {
		if (!(v instanceof Number n) || !Double.isFinite(n.doubleValue()))
			throw bad(q, "must be a number");
		double d = n.doubleValue();
		if (q.min() != null && d < q.min())
			throw bad(q, "must be at least " + q.min());
		if (q.max() != null && d > q.max())
			throw bad(q, "must be at most " + q.max());
		return d;
	}

	private static Object text(Question q, Object v) {
		if (!(v instanceof String s))
			throw bad(q, "must be text");
		String t = s.trim();
		if (t.isEmpty())
			return null; // blank text = cleared
		int max = q.maxLength() == null ? 500 : q.maxLength();
		if (t.length() > max)
			throw bad(q, "is longer than " + max + " characters");
		return t;
	}

	private static Object date(Question q, Object v) {
		if (!(v instanceof String s))
			throw bad(q, "must be a date (YYYY-MM-DD)");
		try {
			return LocalDate.parse(s.trim()).toString();
		} catch (DateTimeParseException e) {
			throw bad(q, "must be a date (YYYY-MM-DD)");
		}
	}

	private static Object bodyMap(Question q, Object v) {
		if (!(v instanceof List<?> list))
			throw bad(q, "expected a list of body areas");
		if (list.size() > 20)
			throw bad(q, "too many areas selected");
		List<Map<String, Object>> out = new ArrayList<>();
		for (Object o : list) {
			if (!(o instanceof Map<?, ?> m) || !(m.get("region") instanceof String region))
				throw bad(q, "each area needs a region");
			Map<String, Object> e = new LinkedHashMap<>();
			try {
				e.put("region", BodyRegion.valueOf(region.trim().toUpperCase()).name());
				if (m.get("side") != null)
					e.put("side", Laterality.valueOf(String.valueOf(m.get("side")).trim().toUpperCase()).name());
			} catch (IllegalArgumentException ex) {
				throw bad(q, "unknown body region or side");
			}
			if (m.get("intensity") != null) {
				if (!(m.get("intensity") instanceof Number n) || n.doubleValue() != Math.rint(n.doubleValue())
						|| n.intValue() < 0 || n.intValue() > 10)
					throw bad(q, "intensity must be 0 to 10");
				e.put("intensity", n.intValue());
			}
			out.add(e);
		}
		return out;
	}

	private static Object angle(Question q, Object v, boolean allowCamera) {
		Object degrees;
		String method = METHOD_MANUAL;
		String captureId = null;
		if (v instanceof Number) {
			degrees = v;
		} else if (v instanceof Map<?, ?> m) {
			degrees = m.get("degrees");
			if (m.get("method") != null) {
				method = String.valueOf(m.get("method")).trim().toUpperCase();
				if (!method.equals(METHOD_MANUAL) && !method.equals(METHOD_CAMERA))
					throw bad(q, "unknown measurement method");
			}
			if (m.get("captureId") != null) {
				try {
					captureId = UUID.fromString(String.valueOf(m.get("captureId"))).toString();
				} catch (IllegalArgumentException e) {
					throw bad(q, "invalid capture id");
				}
			}
		} else {
			throw bad(q, "expected degrees");
		}
		if (!(degrees instanceof Number n) || !Double.isFinite(n.doubleValue()))
			throw bad(q, "needs a number of degrees");
		double d = n.doubleValue();
		if (d < -360 || d > 360)
			throw bad(q, "degrees must be between -360 and 360");
		if (q.min() != null && d < q.min())
			throw bad(q, "must be at least " + q.min());
		if (q.max() != null && d > q.max())
			throw bad(q, "must be at most " + q.max());
		if (method.equals(METHOD_CAMERA) && !allowCamera) {
			throw bad(q, "camera-measured values are confirmed from the capture screen");
		}
		if (method.equals(METHOD_MANUAL) && captureId != null)
			throw bad(q, "a manual value cannot reference a capture");
		Map<String, Object> out = new LinkedHashMap<>();
		out.put("degrees", d);
		out.put("method", method);
		if (captureId != null)
			out.put("captureId", captureId);
		return out;
	}

	// ------------------------------------------------------ walking
	/**
	 * Visits the visible questions of one template in order. A question is visible
	 * only if its showIf holds against the answers of questions that are themselves
	 * visible (hidden parents hide children).
	 * 
	 * @return answers of visible, answered questions
	 */
	public static Map<String, Object> walk(SnapshotTemplate t, Map<String, Object> raw,
			BiConsumer<Question, Object> visit) {
		Map<String, Object> effective = new LinkedHashMap<>();
		Map<String, Object> source = raw == null ? Map.of() : raw;
		for (Section s : t.sections()) {
			for (Question q : s.questions()) {
				if (!isVisible(q, effective))
					continue;
				Object value = source.get(q.key());
				if (isAnswered(q, value))
					effective.put(q.key(), value);
				if (visit != null)
					visit.accept(q, value);
			}
		}
		return effective;
	}

	public static Progress progress(Snapshot snap, Map<String, Map<String, Object>> answers) {
		int[] c = new int[4]; // answered, total, requiredAnswered, requiredTotal
		for (SnapshotTemplate t : snap.templates()) {
			walk(t, answers.get(t.code()), (q, v) -> {
				boolean done = isAnswered(q, v);
				c[1]++;
				if (done)
					c[0]++;
				if (Boolean.TRUE.equals(q.required())) {
					c[3]++;
					if (done)
						c[2]++;
				}
			});
		}
		int percent = c[1] == 0 ? 100 : (int) Math.round(c[0] * 100.0 / c[1]);
		return new Progress(c[0], c[1], c[2], c[3], percent, c[2] == c[3]);
	}

	public static List<String> missingRequired(Snapshot snap, Map<String, Map<String, Object>> answers) {
		List<String> missing = new ArrayList<>();
		for (SnapshotTemplate t : snap.templates()) {
			walk(t, answers.get(t.code()), (q, v) -> {
				if (Boolean.TRUE.equals(q.required()) && !isAnswered(q, v))
					missing.add(t.name() + " > " + q.label());
			});
		}
		return missing;
	}

	/** Drops answers of questions that are hidden (or no longer exist). */
	public static Map<String, Map<String, Object>> pruneHidden(Snapshot snap,
			Map<String, Map<String, Object>> answers) {
		Map<String, Map<String, Object>> out = new LinkedHashMap<>();
		for (SnapshotTemplate t : snap.templates()) {
			Map<String, Object> kept = walk(t, answers.get(t.code()), null);
			if (!kept.isEmpty())
				out.put(t.code(), kept);
		}
		return out;
	}
}