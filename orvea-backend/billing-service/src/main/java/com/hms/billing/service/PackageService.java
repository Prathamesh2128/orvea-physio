package com.hms.billing.service;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hms.billing.dto.BillingDtos.PackageView;
import com.hms.billing.dto.BillingDtos.UsageView;
import com.hms.billing.entity.Invoice;
import com.hms.billing.entity.InvoiceLine;
import com.hms.billing.entity.PackageTemplate;
import com.hms.billing.entity.PackageUsage;
import com.hms.billing.entity.PatientPackage;
import com.hms.billing.entity.ServiceItem;
import com.hms.billing.enums.ChargeStatus;
import com.hms.billing.enums.PackageStatus;
import com.hms.billing.repository.PackageUsageRepository;
import com.hms.billing.repository.PatientPackageRepository;
import com.hms.billing.repository.VisitChargeRepository;
import com.hms.common.audit.AuditPublisher;
import com.hms.common.exception.BusinessException;
import com.hms.common.exception.ResourceNotFoundException;
import com.hms.common.security.ClinicScope;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PackageService {

	private final PatientPackageRepository packages;
	private final PackageUsageRepository usages;
	private final VisitChargeRepository charges;
	private final CatalogService catalog;
	private final AuditPublisher audit;
	private final Clock clock;

	// ============================================= sale
	/** Called when an invoice containing a package is ISSUED. */
	@Transactional
	public PatientPackage createFromSale(Invoice inv, InvoiceLine line, PackageTemplate t) {
		ServiceItem svc = catalog.requireService(t.getServiceId(), false);
		PatientPackage p = new PatientPackage();
		p.setClinicId(inv.getClinicId());
		p.setPatientId(inv.getPatientId());
		p.setPatientName(inv.getPatientName());
		p.setPatientUhid(inv.getPatientUhid());
		p.setCaseId(inv.getCaseId());
		p.setTemplateId(t.getId());
		p.setName(t.getName());
		p.setServiceId(svc.getId());
		p.setServiceName(svc.getName());
		p.setSessionsTotal(t.getSessionCount());
		p.setPrice(line.getLineTotal());
		p.setPurchasedOn(inv.getInvoiceDate());
		p.setValidUntil(inv.getInvoiceDate().plusDays(t.getValidityDays()));
		p.setStatus(PackageStatus.ACTIVE);
		p.setInvoiceId(inv.getId());
		PatientPackage saved = packages.save(p);
		audit.record("PACKAGE_SOLD", "PatientPackage", saved.getId(),
				Map.of("sessions", t.getSessionCount(), "invoiceId", String.valueOf(inv.getId())));
		return saved;
	}

	/**
	 * Invoice cancelled: its packages go too, unless sessions were already used.
	 */
	@Transactional
	public void cancelForInvoice(UUID invoiceId, String reason) {
		for (PatientPackage p : packages.findByInvoiceId(invoiceId)) {
			if (p.getSessionsUsed() > 0 || usages.existsByPackageIdAndReversedFalse(p.getId())) {
				throw new BusinessException("The package '" + p.getName() + "' on this invoice has already been used. "
						+ "Reverse the sessions first if they were recorded by mistake.");
			}
			p.setStatus(PackageStatus.CANCELLED);
			p.setCancelReason(reason);
			packages.save(p);
		}
	}

	// ============================================= use
	/**
	 * Used by billing for a completed visit: the package that expires first is
	 * used.
	 */
	@Transactional
	public Optional<PatientPackage> tryCover(UUID patientId, UUID serviceId, UUID caseId, LocalDate date,
			UUID appointmentId) {
		for (PatientPackage p : packages.findCoverable(patientId, serviceId, date, PackageStatus.ACTIVE)) {
			if (p.getCaseId() != null && caseId != null && !p.getCaseId().equals(caseId))
				continue; // bought for another case
			redeem(p, appointmentId, date, "AUTO", null);
			return Optional.of(p);
		}
		return Optional.empty();
	}

	@Transactional
	public PackageUsage redeem(PatientPackage p, UUID appointmentId, LocalDate usedOn, String source, String note) {
		if (p.getStatus() != PackageStatus.ACTIVE)
			throw new BusinessException("This package is " + p.getStatus());
		if (p.getSessionsUsed() >= p.getSessionsTotal())
			throw new BusinessException("All sessions of this package are used");
		if (p.getValidUntil().isBefore(usedOn))
			throw new BusinessException("This package expired on " + p.getValidUntil());
		p.setSessionsUsed(p.getSessionsUsed() + 1);
		if (p.getSessionsUsed() >= p.getSessionsTotal())
			p.setStatus(PackageStatus.EXHAUSTED);
		packages.save(p);
		PackageUsage u = new PackageUsage();
		u.setClinicId(p.getClinicId());
		u.setPackageId(p.getId());
		u.setAppointmentId(appointmentId);
		u.setUsedOn(usedOn);
		u.setSource(source);
		u.setNote(note);
		return usages.save(u);
	}

	/**
	 * A session recorded by mistake: the session goes back to the package and the
	 * visit becomes billable again.
	 */
	@Transactional
	public PackageView reverseUsage(UUID packageId, UUID usageId, String reason) {
		PatientPackage p = load(packageId);
		PackageUsage u = usages.findByIdAndPackageId(usageId, packageId)
				.orElseThrow(() -> new ResourceNotFoundException("Package usage", usageId));
		if (u.isReversed())
			throw new BusinessException("This session was already reversed");
		if (reason == null || reason.isBlank())
			throw new BusinessException("A reason is required to reverse a session");
		u.setReversed(true);
		u.setReverseReason(reason.trim());
		usages.save(u);
		p.setSessionsUsed(Math.max(0, p.getSessionsUsed() - 1));
		if (p.getStatus() == PackageStatus.EXHAUSTED && !p.getValidUntil().isBefore(LocalDate.now(clock)))
			p.setStatus(PackageStatus.ACTIVE);
		packages.save(p);
		if (u.getAppointmentId() != null) {
			charges.findByAppointmentId(u.getAppointmentId())
					.filter(c -> c.getStatus() == ChargeStatus.COVERED_BY_PACKAGE).ifPresent(c -> {
						c.setStatus(ChargeStatus.PENDING);
						c.setPackageId(null);
						charges.save(c);
					});
		}
		audit.record("PACKAGE_SESSION_REVERSED", "PatientPackage", packageId, Map.of("usageId", usageId.toString()));
		return view(p);
	}

	/** Remaining sessions are voided. Any money back is a separate refund. */
	@Transactional
	public PackageView cancel(UUID packageId, String reason) {
		PatientPackage p = load(packageId);
		if (p.getStatus() != PackageStatus.ACTIVE)
			throw new BusinessException("Only an active package can be cancelled");
		if (reason == null || reason.isBlank())
			throw new BusinessException("A reason is required");
		p.setStatus(PackageStatus.CANCELLED);
		p.setCancelReason(reason.trim());
		packages.save(p);
		audit.record("PACKAGE_CANCELLED", "PatientPackage", packageId,
				Map.of("remaining", p.getSessionsTotal() - p.getSessionsUsed()));
		return view(p);
	}

	// ============================================= read / housekeeping
	@Transactional(readOnly = true)
	public List<PackageView> forPatient(UUID patientId) {
		return packages.findByPatientIdOrderByPurchasedOnDescCreatedAtDesc(patientId).stream()
				.filter(p -> ok(p.getClinicId())).map(this::view).toList();
	}

	@Transactional(readOnly = true)
	public List<UsageView> usagesOf(UUID packageId) {
		load(packageId);
		return usages.findByPackageIdOrderByCreatedAtDesc(packageId).stream().map(u -> new UsageView(u.getId(),
				u.getAppointmentId(), u.getUsedOn(), u.getSource(), u.getNote(), u.isReversed(), u.getReverseReason()))
				.toList();
	}

	@Transactional
	public int expireOverdue(LocalDate today) {
		return packages.expireOverdue(today, PackageStatus.ACTIVE, PackageStatus.EXPIRED);
	}

	private boolean ok(UUID clinicId) {
		UUID caller = ClinicScope.currentClinicId();
		return caller == null || caller.equals(clinicId);
	}

	PatientPackage load(UUID id) {
		PatientPackage p = packages.findById(id).orElseThrow(() -> new ResourceNotFoundException("Package", id));
		ClinicScope.assertSameClinic(p.getClinicId(), "Package", id);
		return p;
	}

	PackageView view(PatientPackage p) {
		return new PackageView(p.getId(), p.getPatientId(), p.getPatientName(), p.getName(), p.getServiceId(),
				p.getServiceName(), p.getSessionsTotal(), p.getSessionsUsed(),
				p.getSessionsTotal() - p.getSessionsUsed(), p.getPurchasedOn(), p.getValidUntil(), p.getStatus(),
				p.getInvoiceId(), p.getCaseId(), p.getPrice());
	}

	public PackageView viewOf(UUID packageId) {
		return view(load(packageId));
	}
}