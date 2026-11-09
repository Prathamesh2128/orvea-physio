package com.hms.appointment.service;

import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hms.appointment.dto.NextVisit;
import com.hms.appointment.entity.Appointment;
import com.hms.appointment.enums.AppointmentStatus;
import com.hms.appointment.repository.AppointmentRepository;
import com.hms.common.security.ClinicScope;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class NextVisitService {

	static final int MAX_PATIENTS = 200;
	private static final List<AppointmentStatus> UPCOMING = List.of(AppointmentStatus.BOOKED,
			AppointmentStatus.CONFIRMED, AppointmentStatus.CHECKED_IN, AppointmentStatus.IN_PROGRESS);

	private final AppointmentRepository repository;
	private final Clock clock;

	/** The next upcoming visit of each patient (at most one row per patient). */
	@Transactional(readOnly = true)
	public List<NextVisit> nextVisits(Collection<UUID> patientIds) {
		if (patientIds == null || patientIds.isEmpty())
			return List.of();
		List<UUID> ids = patientIds.stream().distinct().limit(MAX_PATIENTS).toList();
		UUID clinic = ClinicScope.currentClinicId();
		Map<UUID, NextVisit> first = new LinkedHashMap<>();
		for (Appointment a : repository.findUpcomingForPatients(ids, LocalDate.now(clock), UPCOMING)) {
			if (clinic != null && !clinic.equals(a.getClinicId()))
				continue;
			first.putIfAbsent(a.getPatientId(), new NextVisit(a.getPatientId(), a.getId(), a.getProviderId(),
					a.getProviderName(), a.getAppointmentDate(), a.getStartTime()));
		}
		return new ArrayList<>(first.values());
	}
}