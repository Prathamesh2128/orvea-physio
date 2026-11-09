package com.hms.appointment.repository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.hms.appointment.entity.Appointment;
import com.hms.appointment.enums.AppointmentStatus;

public interface AppointmentRepository extends JpaRepository<Appointment, UUID> {

	@Query("""
			select a from Appointment a
			where a.providerId = :pid and a.appointmentDate = :date and a.status in :statuses
			order by a.startTime
			""")
	List<Appointment> findBlocking(@Param("pid") UUID providerId, @Param("date") LocalDate date,
			@Param("statuses") Collection<AppointmentStatus> statuses);

	@Query("""
			select count(a) from Appointment a
			where a.patientId = :pid and a.appointmentDate = :date and a.status in :statuses
			  and a.startTime < :end and a.endTime > :start
			""")
	long countPatientOverlap(@Param("pid") UUID patientId, @Param("date") LocalDate date,
			@Param("start") LocalTime start, @Param("end") LocalTime end,
			@Param("statuses") Collection<AppointmentStatus> statuses);

	@Query("""
			select count(a) from Appointment a
			where a.providerId = :pid and a.appointmentDate between :from and :to and a.status in :statuses
			""")
	long countProviderBetween(@Param("pid") UUID providerId, @Param("from") LocalDate from, @Param("to") LocalDate to,
			@Param("statuses") Collection<AppointmentStatus> statuses);

	@Query("""
			select count(a) from Appointment a
			where a.providerId = :pid and a.appointmentDate = :date and a.status in :statuses
			  and a.startTime < :end and a.endTime > :start
			""")
	long countProviderOverlap(@Param("pid") UUID providerId, @Param("date") LocalDate date,
			@Param("start") LocalTime start, @Param("end") LocalTime end,
			@Param("statuses") Collection<AppointmentStatus> statuses);

	@Query("""
			select a from Appointment a
			where (:clinicId is null or a.clinicId = :clinicId)
			  and (:date is null or a.appointmentDate = :date)
			  and (:providerId is null or a.providerId = :providerId)
			  and (:patientId is null or a.patientId = :patientId)
			  and (:status is null or a.status = :status)
			""")
	Page<Appointment> search(@Param("date") LocalDate date, @Param("providerId") UUID providerId,
			@Param("patientId") UUID patientId, @Param("status") AppointmentStatus status,
			@Param("clinicId") UUID clinicId, Pageable pageable);

	@Query("""
			select a from Appointment a
			where a.patientId in :ids and a.appointmentDate >= :from and a.status in :statuses
			order by a.appointmentDate, a.startTime
			""")
	List<Appointment> findUpcomingForPatients(@Param("ids") Collection<UUID> ids, @Param("from") LocalDate from,
			@Param("statuses") Collection<AppointmentStatus> statuses);
}
