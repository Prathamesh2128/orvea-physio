package com.hms.assessment.config;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.hms.assessment.engine.TemplateDefinition;
import com.hms.assessment.engine.TemplateValidator;
import com.hms.assessment.entity.AssessmentTemplate;
import com.hms.assessment.enums.TemplateStatus;
import com.hms.assessment.repository.AssessmentTemplateRepository;
import com.hms.assessment.util.Json;
import com.hms.common.exception.BusinessException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Starter forms (shared, clinic_id NULL). Clinics customise them; nothing here
 * is a clinical rule.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TemplateSeeder implements ApplicationRunner {

	public static final List<String> SEED_FILES = List.of("COMMON", "LYMPHEDEMA", "KNEE", "SHOULDER", "BACK_NECK",
			"POST_SURGERY");

	public record SeedFile(String code, String name, String description, TemplateDefinition definition) {
	}

	private final AssessmentTemplateRepository repository;

	@Override
	@Transactional
	public void run(ApplicationArguments args) throws BusinessException {
		for (String file : SEED_FILES) {
			try {
				SeedFile seed = read(file);
				if (!repository.findByClinicIdAndCodeOrderByTemplateVersionDesc(null, seed.code()).isEmpty())
					continue;
				TemplateValidator.validate(seed.definition());
				AssessmentTemplate t = new AssessmentTemplate();
				t.setClinicId(null);
				t.setCode(seed.code());
				t.setName(seed.name());
				t.setDescription(seed.description());
				t.setDefinition(Json.write(seed.definition()));
				t.setTemplateVersion(1);
				t.setStatus(TemplateStatus.PUBLISHED);
				t.setPublishedAt(LocalDateTime.now());
				repository.save(t);
				log.info("Seeded shared assessment template {}", seed.code());
			} catch (java.io.IOException | RuntimeException e) {
				log.error("Could not seed assessment template {}: {}", file, e.getMessage()); // do not stop the service
			}
		}
	}

	public static SeedFile read(String code) throws java.io.IOException {
		try (InputStream in = new ClassPathResource("template-seeds/" + code + ".json").getInputStream()) {
			return Json.readStrict(new String(in.readAllBytes(), StandardCharsets.UTF_8), SeedFile.class);
		}
	}
}