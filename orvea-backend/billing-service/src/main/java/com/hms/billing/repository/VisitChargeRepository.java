package com.hms.billing.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.hms.billing.entity.VisitCharge;
import com.hms.billing.enums.ChargeStatus;

public interface VisitChargeRepository extends JpaRepository<VisitCharge, UUID> {

	boolean existsByAppointmentId(UUID appointmentId);

	Optional<VisitCharge> findByAppointmentId(UUID appointmentId);

	List<VisitCharge> findByInvoiceId(UUID invoiceId);

	@Query("""
			select c from VisitCharge c
			where (:clinicId is null or c.clinicId = :clinicId)
			  and (:patientId is null or c.patientId = :patientId)
			  and (:status is null or c.status = :status)
			""")
	Page<VisitCharge> search(@Param("patientId") UUID patientId, @Param("status") ChargeStatus status,
			@Param("clinicId") UUID clinicId, Pageable pageable);
}