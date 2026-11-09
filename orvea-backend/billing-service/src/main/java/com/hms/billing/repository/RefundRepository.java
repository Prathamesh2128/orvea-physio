package com.hms.billing.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.hms.billing.entity.Refund;

public interface RefundRepository extends JpaRepository<Refund, UUID> {

	List<Refund> findByInvoiceIdOrderByCreatedAtAsc(UUID invoiceId);

	/** Rows: mode, total refunded. */
	@Query("""
			select r.mode, sum(r.amount) from Refund r
			where r.refundedOn between :from and :to and (:clinicId is null or r.clinicId = :clinicId)
			group by r.mode
			""")
	List<Object[]> refundedByMode(@Param("from") LocalDate from, @Param("to") LocalDate to,
			@Param("clinicId") UUID clinicId);
}