package com.hms.patient.service;

import java.time.Year;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.hms.patient.entity.UhidCounter;
import com.hms.patient.repository.UhidCounterRepository;

@Service
public class UhidGenerator {
	private final UhidCounterRepository uhidCounterRepository;
	private final String prefix;

	public UhidGenerator(UhidCounterRepository uhidCounterRepository,
			@Value("${physio.patient.uhid-prefix:PH}") String prefix) {
		this.uhidCounterRepository = uhidCounterRepository;
		this.prefix = prefix;
	}

	public String generate(UUID clinicId) {
		int year = Year.now().getValue();
		String key = (clinicId == null ? "DEFAULT" : clinicId.toString()) + ":" + year;
		uhidCounterRepository.initIfAbsent(key);
		UhidCounter counter = uhidCounterRepository.findForUpdate(key).orElseThrow();
		counter.setLastNumber(counter.getLastNumber() + 1);
		uhidCounterRepository.save(counter);
		return String.format("%s-%d-%06d", prefix, year, counter.getLastNumber());
	}
}