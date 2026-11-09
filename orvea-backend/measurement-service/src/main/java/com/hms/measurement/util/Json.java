package com.hms.measurement.util;

import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.json.JsonMapper;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class Json {
	private Json() {
	}

	private static final JsonMapper STRICT = JsonMapper.builder()
			.enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES).build();
	private static final JsonMapper LENIENT = JsonMapper.builder()
			.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES).build();

	public static String write(Object value) {
		return LENIENT.writeValueAsString(value);
	}

	public static <T> T readStrict(String json, Class<T> type) {
		return STRICT.readValue(json, type);
	}

	public static <T> T read(String json, Class<T> type) {
		return LENIENT.readValue(json, type);
	}

	@SuppressWarnings("unchecked")
	public static Map<String, Object> readMap(String json) {
		if (json == null || json.isBlank())
			return new LinkedHashMap<>();
		return LENIENT.readValue(json, LinkedHashMap.class);
	}

	/** {"TEMPLATE": {"question": value}} */
	@SuppressWarnings("unchecked")
	public static Map<String, Map<String, Object>> readAnswers(String json) {
		Map<String, Map<String, Object>> out = new LinkedHashMap<>();
		readMap(json).forEach((k, v) -> out.put(k,
				v instanceof Map<?, ?> m ? new LinkedHashMap<>((Map<String, Object>) m) : new LinkedHashMap<>()));
		return out;
	}

	@SuppressWarnings("unchecked")
	public static List<String> readStringList(String json) {
		if (json == null || json.isBlank())
			return new ArrayList<>();
		return LENIENT.readValue(json, ArrayList.class);
	}
}