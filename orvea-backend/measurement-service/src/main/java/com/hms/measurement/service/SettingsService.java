package com.hms.measurement.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hms.common.audit.AuditPublisher;
import com.hms.common.exception.BusinessException;
import com.hms.common.security.ClinicScope;
import com.hms.measurement.dto.SettingsDtos.SettingsRequest;
import com.hms.measurement.dto.SettingsDtos.SettingsResponse;
import com.hms.measurement.entity.MeasurementSettings;
import com.hms.measurement.repository.SettingsRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SettingsService {

	private final SettingsRepository repository;
	private final AuditPublisher audit;

	/**
	 * Get-or-create with the defaults (5 days, from actual date, 3-day window, no
	 * flag threshold).
	 */
	@Transactional
	public MeasurementSettings effective(UUID clinicId) {
		return repository.findByClinicId(clinicId).orElseGet(() -> {
			MeasurementSettings s = new MeasurementSettings();
			s.setClinicId(clinicId);
			return repository.save(s);
		});
	}

	@Transactional
	public SettingsResponse get() {
		return toResponse(effective(ClinicScope.currentClinicId()));
	}

	@Transactional
	public SettingsResponse update(SettingsRequest r) {
		MeasurementSettings s = effective(ClinicScope.currentClinicId());
		Map<String, Object> changed = new LinkedHashMap<>();
		if (r.defaultRepeatDays() != null) {
			s.setDefaultRepeatDays(checkRepeat(r.defaultRepeatDays()));
			changed.put("defaultRepeatDays", r.defaultRepeatDays());
		}
		if (r.defaultDueMode() != null) {
			s.setDefaultDueMode(r.defaultDueMode());
			changed.put("defaultDueMode", r.defaultDueMode().name());
		}
		if (r.comingUpDays() != null) {
			if (r.comingUpDays() < 0 || r.comingUpDays() > 30)
				throw new BusinessException("The 'coming up' window must be 0 to 30 days");
			s.setComingUpDays(r.comingUpDays());
			changed.put("comingUpDays", r.comingUpDays());
		}
		if (r.trendMetric() != null) {
			s.setTrendMetric(r.trendMetric());
			changed.put("trendMetric", r.trendMetric().name());
		}
		if (r.trendTolerancePercent() != null) {
			if (r.trendTolerancePercent().signum() < 0
					|| r.trendTolerancePercent().compareTo(new BigDecimal("100")) > 0) {
				throw new BusinessException("The trend tolerance must be between 0 and 100");
			}
			s.setTrendTolerancePercent(r.trendTolerancePercent().setScale(2, RoundingMode.HALF_UP));
			changed.put("trendTolerancePercent", s.getTrendTolerancePercent());
		}
		if (Boolean.TRUE.equals(r.clearFlagThreshold())) {
			s.setFlagThresholdPercent(null);
			changed.put("flagThresholdPercent", "cleared");
		} else if (r.flagThresholdPercent() != null) {
			if (r.flagThresholdPercent().signum() < 0
					|| r.flagThresholdPercent().compareTo(new BigDecimal("1000")) > 0) {
				throw new BusinessException("The flag level must be between 0 and 1000 percent");
			}
			s.setFlagThresholdPercent(r.flagThresholdPercent().setScale(2, RoundingMode.HALF_UP));
			changed.put("flagThresholdPercent", s.getFlagThresholdPercent());
		}
		if (r.patientReminderLeadDays() != null) {
			if (r.patientReminderLeadDays() < 0 || r.patientReminderLeadDays() > 14)
				throw new BusinessException("Reminder lead time must be 0 to 14 days");
			s.setPatientReminderLeadDays(r.patientReminderLeadDays());
			changed.put("patientReminderLeadDays", r.patientReminderLeadDays());
		}
		audit.record("MEASUREMENT_SETTINGS_CHANGED", "MeasurementSettings", s.getId(), changed);
		return toResponse(s);
	}

	public static int checkRepeat(int days) {
		if (days < 1 || days > 90)
			throw new BusinessException("Repeat interval must be 1 to 90 days");
		return days;
	}

	private SettingsResponse toResponse(MeasurementSettings s) {
		return new SettingsResponse(s.getDefaultRepeatDays(), s.getDefaultDueMode(), s.getComingUpDays(),
				s.getTrendMetric(), s.getTrendTolerancePercent(), s.getFlagThresholdPercent(),
				s.getPatientReminderLeadDays());
	}
}