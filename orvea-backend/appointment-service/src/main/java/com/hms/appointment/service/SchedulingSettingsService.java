package com.hms.appointment.service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hms.appointment.dto.SchedulingSettingsRequest;
import com.hms.appointment.dto.SchedulingSettingsResponse;
import com.hms.appointment.entity.ClinicSchedulingSettings;
import com.hms.appointment.enums.ReminderChannel;
import com.hms.appointment.repository.ClinicSchedulingSettingsRepository;
import com.hms.appointment.util.SchedulingRules;
import com.hms.common.exception.BusinessException;
import com.hms.common.security.ClinicScope;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SchedulingSettingsService {

	private final ClinicSchedulingSettingsRepository repository;

	/**
	 * Get-or-create: a clinic that never configured anything gets sensible
	 * defaults.
	 */
	@Transactional
	public ClinicSchedulingSettings effective(UUID clinicId) {
		return repository.findByClinicId(clinicId).orElseGet(() -> repository.save(defaults(clinicId)));
	}

	@Transactional
	public SchedulingSettingsResponse get() {
		return toResponse(effective(ClinicScope.currentClinicId()));
	}

	/**
	 * Changes apply to new bookings only; reminders already scheduled are not
	 * rewritten.
	 */
	@Transactional
	public SchedulingSettingsResponse update(SchedulingSettingsRequest r) {
		ClinicSchedulingSettings s = effective(ClinicScope.currentClinicId());
		if (r.defaultSlotMinutes() != null)
			s.setDefaultSlotMinutes(SchedulingRules.requireSlot(r.defaultSlotMinutes()));
		if (r.defaultMaxParallel() != null)
			s.setDefaultMaxParallel(SchedulingRules.requireMaxParallel(r.defaultMaxParallel()));
		if (r.maxAdvanceBookingDays() != null) {
			if (r.maxAdvanceBookingDays() < 1 || r.maxAdvanceBookingDays() > 365) {
				throw new BusinessException("Advance booking window must be 1 to 365 days");
			}
			s.setMaxAdvanceBookingDays(r.maxAdvanceBookingDays());
		}
		if (r.reminderEnabled() != null)
			s.setReminderEnabled(r.reminderEnabled());
		if (r.reminderOffsetsMinutes() != null) {
			List<Integer> offsets = r.reminderOffsetsMinutes().stream().distinct().sorted((a, b) -> b - a).toList();
			if (offsets.size() > 5)
				throw new BusinessException("At most 5 reminders per appointment");
			if (offsets.stream().anyMatch(o -> o < 5 || o > 10080)) {
				throw new BusinessException("Reminder offset must be between 5 minutes and 7 days");
			}
			s.getReminderOffsetsMinutes().clear();
			s.getReminderOffsetsMinutes().addAll(offsets);
		}
		if (r.reminderChannels() != null) {
			List<ReminderChannel> channels = r.reminderChannels().stream().distinct().toList();
			s.getReminderChannels().clear();
			s.getReminderChannels().addAll(channels);
		}
		if (s.isReminderEnabled() && s.getReminderChannels().isEmpty()) {
			throw new BusinessException("Choose at least one reminder channel or turn reminders off");
		}
		return toResponse(s);
	}

	private ClinicSchedulingSettings defaults(UUID clinicId) {
		ClinicSchedulingSettings s = new ClinicSchedulingSettings();
		s.setClinicId(clinicId);
		s.getReminderOffsetsMinutes().addAll(List.of(1440, 120)); // 24 h and 2 h before
		s.getReminderChannels().addAll(List.of(ReminderChannel.WHATSAPP, ReminderChannel.SMS));
		return s;
	}

	private SchedulingSettingsResponse toResponse(ClinicSchedulingSettings s) {
		return new SchedulingSettingsResponse(s.getDefaultSlotMinutes(), s.getDefaultMaxParallel(),
				s.getMaxAdvanceBookingDays(), s.isReminderEnabled(), new ArrayList<>(s.getReminderOffsetsMinutes()),
				new ArrayList<>(s.getReminderChannels()));
	}
}
