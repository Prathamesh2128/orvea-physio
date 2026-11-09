package com.hms.billing.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hms.billing.dto.BillingDtos.ChargeView;
import com.hms.billing.entity.PatientPackage;
import com.hms.billing.entity.ServiceItem;
import com.hms.billing.entity.VisitCharge;
import com.hms.billing.enums.ChargeStatus;
import com.hms.billing.repository.VisitChargeRepository;
import com.hms.common.api.PageResponse;
import com.hms.common.audit.AuditPublisher;
import com.hms.common.event.BaseEvent;
import com.hms.common.exception.BusinessException;
import com.hms.common.exception.ResourceNotFoundException;
import com.hms.common.security.ClinicScope;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChargeService {

	private final VisitChargeRepository charges;
	private final CatalogService catalog;
	private final PackageService packageService;
	private final AuditPublisher audit;

	// ============================================== from appointment-service
	/**
	 * A visit was completed: use a package session, or leave a pending charge. Safe
	 * to receive twice.
	 */
	@Transactional
	public void onVisitCompleted(BaseEvent e) {
		UUID appointmentId = e.uuid("appointmentId");
		UUID patientId = e.uuid("patientId");
		if (appointmentId == null || patientId == null) {
			log.warn("Ignoring a completed-visit event without appointment or patient id");
			return;
		}
		if (charges.existsByAppointmentId(appointmentId))
			return;
		LocalDate date;
		try {
			date = LocalDate.parse(e.text("appointmentDate"));
		} catch (RuntimeException ex) {
			log.warn("Ignoring a completed-visit event with a bad date: {}", e.text("appointmentDate"));
			return;
		}
		UUID clinic = e.getClinicId();
		UUID caseId = e.uuid("caseId");

		VisitCharge c = new VisitCharge();
		c.setClinicId(clinic);
		c.setAppointmentId(appointmentId);
		c.setPatientId(patientId);
		c.setPatientName(e.text("patientName"));
		c.setPatientUhid(e.text("patientUhid"));
		c.setCaseId(caseId);
		c.setProviderId(e.uuid("providerId"));
		c.setProviderName(e.text("providerName"));
		c.setVisitTypeCode(e.text("visitTypeCode"));
		c.setServiceDate(date);

		Optional<ServiceItem> svc = catalog.findForVisitType(clinic, e.text("visitTypeCode"));
		if (svc.isEmpty()) {
			c.setServiceName(e.text("visitTypeName"));
			c.setStatus(ChargeStatus.UNMAPPED); // nothing is silently lost
		} else {
			applyService(c, svc.get());
			if (c.getUnitPrice().signum() == 0) {
				c.setStatus(ChargeStatus.NO_CHARGE);
			} else {
				Optional<PatientPackage> pkg = packageService.tryCover(patientId, svc.get().getId(), caseId, date,
						appointmentId);
				if (pkg.isPresent()) {
					c.setStatus(ChargeStatus.COVERED_BY_PACKAGE);
					c.setPackageId(pkg.get().getId());
				} else {
					c.setStatus(ChargeStatus.PENDING);
				}
			}
		}
		charges.save(c);
	}

	private void applyService(VisitCharge c, ServiceItem s) {
		c.setServiceId(s.getId());
		c.setServiceName(s.getName());
		c.setUnitPrice(s.getPrice());
		c.setTaxRatePercent(s.getTaxRatePercent());
	}

	// ============================================== staff actions
	@Transactional(readOnly = true)
	public PageResponse<ChargeView> list(UUID patientId, ChargeStatus status, Pageable pageable) {
		return PageResponse.from(charges.search(patientId, status, ClinicScope.currentClinicId(), pageable),
				this::view);
	}

	/** Free visit (for example a review), with a reason. */
	@Transactional
	public ChargeView waive(UUID id, String reason) {
		VisitCharge c = load(id);
		if (c.getStatus() != ChargeStatus.PENDING && c.getStatus() != ChargeStatus.UNMAPPED) {
			throw new BusinessException("Only a pending visit can be waived (this one is " + c.getStatus() + ")");
		}
		if (reason == null || reason.isBlank())
			throw new BusinessException("A reason is required to waive a charge");
		c.setStatus(ChargeStatus.WAIVED);
		c.setNote(reason.trim());
		audit.record("CHARGE_WAIVED", "VisitCharge", id, Map.of("appointmentId", c.getAppointmentId().toString()));
		return view(charges.save(c));
	}

	/**
	 * Link a visit that had no fee to a service (it is then priced, and a package
	 * may cover it).
	 */
	@Transactional
	public ChargeView assignService(UUID id, UUID serviceId) {
		VisitCharge c = load(id);
		if (c.getStatus() != ChargeStatus.UNMAPPED && c.getStatus() != ChargeStatus.PENDING) {
			throw new BusinessException("The service can only be set on a pending or unmatched visit");
		}
		ServiceItem svc = catalog.requireService(serviceId, true);
		applyService(c, svc);
		if (svc.getPrice().compareTo(BigDecimal.ZERO) == 0) {
			c.setStatus(ChargeStatus.NO_CHARGE);
		} else {
			c.setStatus(ChargeStatus.PENDING);
			packageService
					.tryCover(c.getPatientId(), svc.getId(), c.getCaseId(), c.getServiceDate(), c.getAppointmentId())
					.ifPresent(p -> {
						c.setStatus(ChargeStatus.COVERED_BY_PACKAGE);
						c.setPackageId(p.getId());
					});
		}
		return view(charges.save(c));
	}

	/** Use one session of a chosen package for a pending visit. */
	@Transactional
	public ChargeView coverWithPackage(UUID id, UUID packageId) {
		VisitCharge c = load(id);
		if (c.getStatus() != ChargeStatus.PENDING)
			throw new BusinessException("Only a pending visit can use a package session");
		PatientPackage p = packageService.load(packageId);
		if (!p.getPatientId().equals(c.getPatientId()))
			throw new BusinessException("This package belongs to a different patient");
		if (c.getServiceId() == null || !c.getServiceId().equals(p.getServiceId())) {
			throw new BusinessException("This package is for '" + p.getServiceName() + "', not for this visit");
		}
		packageService.redeem(p, c.getAppointmentId(), c.getServiceDate(), "MANUAL", "Applied by staff");
		c.setStatus(ChargeStatus.COVERED_BY_PACKAGE);
		c.setPackageId(p.getId());
		return view(charges.save(c));
	}

	private VisitCharge load(UUID id) {
		VisitCharge c = charges.findById(id).orElseThrow(() -> new ResourceNotFoundException("Charge", id));
		ClinicScope.assertSameClinic(c.getClinicId(), "Charge", id);
		return c;
	}

	private ChargeView view(VisitCharge c) {
		return new ChargeView(c.getId(), c.getAppointmentId(), c.getPatientId(), c.getPatientName(), c.getPatientUhid(),
				c.getCaseId(), c.getProviderName(), c.getServiceId(), c.getServiceName(), c.getServiceDate(),
				c.getUnitPrice(), c.getTaxRatePercent(), c.getStatus(), c.getPackageId(), c.getInvoiceId(),
				c.getNote());
	}
}