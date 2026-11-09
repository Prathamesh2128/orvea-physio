package com.hms.billing.repository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.hms.billing.entity.Invoice;
import com.hms.billing.enums.InvoiceStatus;

import jakarta.persistence.LockModeType;

public interface InvoiceRepository extends JpaRepository<Invoice, UUID> {

	/**
	 * Serialises payments on one invoice so two receptionists cannot overpay it.
	 */
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select i from Invoice i where i.id = :id")
	Optional<Invoice> lockById(@Param("id") UUID id);

	@Query("""
			select i from Invoice i
			where (:clinicId is null or i.clinicId = :clinicId)
			  and (:patientId is null or i.patientId = :patientId)
			  and (:status is null or i.status = :status)
			  and (:from is null or i.invoiceDate >= :from)
			  and (:to is null or i.invoiceDate <= :to)
			""")
	Page<Invoice> search(@Param("patientId") UUID patientId, @Param("status") InvoiceStatus status,
			@Param("from") LocalDate from, @Param("to") LocalDate to, @Param("clinicId") UUID clinicId,
			Pageable pageable);

	@Query("select i from Invoice i where i.status in :statuses and (:clinicId is null or i.clinicId = :clinicId) order by i.invoiceDate")
	List<Invoice> findByStatuses(@Param("statuses") Collection<InvoiceStatus> statuses,
			@Param("clinicId") UUID clinicId);
}