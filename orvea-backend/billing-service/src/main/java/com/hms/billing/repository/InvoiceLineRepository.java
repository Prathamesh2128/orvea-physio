package com.hms.billing.repository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.hms.billing.entity.InvoiceLine;
import com.hms.billing.enums.InvoiceStatus;

public interface InvoiceLineRepository extends JpaRepository<InvoiceLine, UUID> {

	/** Rows: type, item name, quantity, taxable, tax, total. */
	@Query("""
			select l.type, l.itemName, sum(l.quantity), sum(l.taxableAmount), sum(l.taxAmount), sum(l.lineTotal)
			from InvoiceLine l
			where l.invoice.status in :statuses and l.invoice.invoiceDate between :from and :to
			  and (:clinicId is null or l.invoice.clinicId = :clinicId)
			group by l.type, l.itemName
			order by sum(l.lineTotal) desc
			""")
	List<Object[]> revenueByItem(@Param("statuses") Collection<InvoiceStatus> statuses, @Param("from") LocalDate from,
			@Param("to") LocalDate to, @Param("clinicId") UUID clinicId);
}