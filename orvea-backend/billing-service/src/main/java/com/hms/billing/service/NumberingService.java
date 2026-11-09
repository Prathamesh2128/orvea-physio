package com.hms.billing.service;

import java.time.LocalDate;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hms.billing.engine.FiscalYear;
import com.hms.billing.entity.BillingCounter;
import com.hms.billing.repository.BillingCounterRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class NumberingService {

	private final BillingCounterRepository repository;

	/**
	 * Gapless within the transaction: the counter row is locked until commit. A new
	 * financial year restarts at 1.
	 */
	@Transactional
	public String next(String prefix, UUID clinicId, LocalDate date, int fyStartMonth) {
		String fy = FiscalYear.label(date, fyStartMonth);
		String key = prefix + ":" + (clinicId == null ? "DEFAULT" : clinicId) + ":" + fy;
		repository.initIfAbsent(key);
		BillingCounter c = repository.findForUpdate(key).orElseThrow();
		c.setLastNumber(c.getLastNumber() + 1);
		repository.save(c);
		return FiscalYear.number(prefix, fy, c.getLastNumber());
	}
}