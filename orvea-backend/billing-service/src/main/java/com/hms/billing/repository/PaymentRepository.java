package com.hms.billing.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.hms.billing.entity.Payment;

public interface PaymentRepository extends JpaRepository<Payment, UUID> {

	List<Payment> findByInvoiceIdOrderByCreatedAtAsc(UUID invoiceId);

	/** Rows: mode, total received, count. */
	@Query("""
			select p.mode, sum(p.amount), count(p) from Payment p
			where p.receivedOn between :from and :to and (:clinicId is null or p.clinicId = :clinicId)
			group by p.mode
			""")
	List<Object[]> receivedByMode(@Param("from") LocalDate from, @Param("to") LocalDate to,
			@Param("clinicId") UUID clinicId);
}