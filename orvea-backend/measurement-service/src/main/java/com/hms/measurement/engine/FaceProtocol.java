package com.hms.measurement.engine;

import java.math.BigDecimal;
import java.util.List;

/** The face template is fixed. */
public final class FaceProtocol {
	private FaceProtocol() {
	}

	public record Item(String key, String label) {
	}

	public static final List<Item> LANDMARKS = List.of(new Item("tragus_to_mouth_corner", "Ear tragus to mouth corner"),
			new Item("eye_outer_corner_to_jaw_angle", "Outer eye corner to jaw angle"));

	public static final List<Item> ANGLES = List.of(new Item("mouth_line_tilt", "Mouth line tilt"),
			new Item("eyebrow_line_tilt", "Eyebrow line tilt"));

	public static final BigDecimal MIN_ANGLE = new BigDecimal("-90");
	public static final BigDecimal MAX_ANGLE = new BigDecimal("90");

	public static String landmarkLabel(String key) {
		return LANDMARKS.stream().filter(i -> i.key().equals(key)).map(Item::label).findFirst().orElse(null);
	}

	public static String angleLabel(String key) {
		return ANGLES.stream().filter(i -> i.key().equals(key)).map(Item::label).findFirst().orElse(null);
	}
}