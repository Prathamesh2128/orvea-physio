package com.hms.common.event;

import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.json.JsonMapper;

public class EventJson {
	private EventJson() { }

	private static final JsonMapper MAPPER = JsonMapper.builder()
			.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES).build();

	public static String write(Object value) {
		return MAPPER.writeValueAsString(value);
	}

	public static <T> T read(String json, Class<T> type) {
		return MAPPER.readValue(json, type);
	}
}
