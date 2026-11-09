package com.hms.measurement.enums;

public enum Landmark {
	WRIST("wrist"), ELBOW_CREASE("elbow crease"), ANKLE_BONE("ankle bone"), KNEE_CAP("knee cap");

	private final String label;

	Landmark(String label) {
		this.label = label;
	}

	public String label() {
		return label;
	}
}
