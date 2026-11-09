package com.hms.billing.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.hms.billing.entity.PatientPackage;
import com.hms.billing.enums.PackageStatus;

public interface PatientPackageRepository extends JpaRepository<PatientPackage, UUID> {

	List<PatientPackage> findByPatientIdOrderByPurchasedOnDescCreatedAtDesc(UUID patientId);

	List<PatientPackage> findByInvoiceId(UUID invoiceId);

	/** Usable packages for this service, the one expiring first comes first. */
	@Query("""
			select p from PatientPackage p
			where p.patientId = :patientId and p.serviceId = :serviceId and p.status = :status
			  and p.sessionsUsed < p.sessionsTotal and p.validUntil >= :date
			order by p.validUntil, p.purchasedOn
			""")
	List<PatientPackage> findCoverable(@Param("patientId") UUID patientId, @Param("serviceId") UUID serviceId,
			@Param("date") LocalDate date, @Param("status") PackageStatus status);

	@Modifying(clearAutomatically = true)
	@Query("update PatientPackage p set p.status = :to where p.status = :from and p.validUntil < :today")
	int expireOverdue(@Param("today") LocalDate today, @Param("from") PackageStatus from,
			@Param("to") PackageStatus to);
}