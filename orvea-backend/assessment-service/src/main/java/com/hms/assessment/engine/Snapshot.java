package com.hms.assessment.engine;

import java.util.List;

/** The exact forms an assessment was started with (frozen copy). */
public record Snapshot(List<SnapshotTemplate> templates) {
	public record SnapshotTemplate(String code, String name, Integer version,
			List<TemplateDefinition.Section> sections) {
	}
}