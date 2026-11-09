package com.hms.measurement.config;

import java.math.BigDecimal;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.hms.measurement.entity.ProtocolTemplate;
import com.hms.measurement.enums.Landmark;
import com.hms.measurement.enums.LimbType;
import com.hms.measurement.repository.ProtocolTemplateRepository;

import lombok.RequiredArgsConstructor;

/**
 * Two starter presets so the screen is not empty. Clinics and therapists create
 * their own.
 */
@Component
@RequiredArgsConstructor
public class ProtocolSeeder implements ApplicationRunner {

	private final ProtocolTemplateRepository repository;

	@Override
	@Transactional
	public void run(ApplicationArguments args) {
		if (repository.count() > 0)
			return;
		seed("Arm: every 5 cm from the wrist (5 points)", LimbType.ARM, "5", Landmark.WRIST, 5);
		seed("Leg: every 10 cm from the ankle bone (5 points)", LimbType.LEG, "10", Landmark.ANKLE_BONE, 5);
	}

	private void seed(String name, LimbType limb, String step, Landmark landmark, int points) {
		ProtocolTemplate t = new ProtocolTemplate();
		t.setName(name);
		t.setLimbType(limb);
		t.setStepCm(new BigDecimal(step).setScale(2));
		t.setStartLandmark(landmark);
		t.setPointCount(points);
		repository.save(t);
	}
}