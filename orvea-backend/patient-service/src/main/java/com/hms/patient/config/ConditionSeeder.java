package com.hms.patient.config;

import java.util.List;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.transaction.annotation.Transactional;

import com.hms.patient.entity.ConditionMaster;
import com.hms.patient.repository.ConditionMasterRepository;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class ConditionSeeder implements ApplicationRunner {
	
	
	private final ConditionMasterRepository conditionMasterRepository;

	private record Seed(String code, String name, String category, int order) { }

	@Override
	@Transactional
	public void run(ApplicationArguments args) throws Exception {
		List<Seed> seeds = List.of(new Seed("LYMPHEDEMA", "Lymphedema", "ONCOLOGY", 10),
				new Seed("KNEE", "Knee condition", "ORTHO", 20),
				new Seed("SHOULDER", "Shoulder condition", "ORTHO", 30),
				new Seed("BACK_NECK", "Back and neck", "ORTHO", 40),
				new Seed("POST_SURGERY", "After surgery", "REHAB", 50),
				new Seed("FACIAL", "Facial condition", "NEURO", 60));

		for (Seed s : seeds) {
			if(!conditionMasterRepository.existsByCodeIgnoreCase(s.code())) {
				ConditionMaster m = new ConditionMaster();
                m.setCode(s.code());
                m.setName(s.name());
                m.setCategory(s.category());
                m.setSortOrder(s.order());
                conditionMasterRepository.save(m);
			}
		}
	}

}
