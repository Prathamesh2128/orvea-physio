package com.hms.billing.service;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hms.billing.dto.BillingDtos.PackageTemplateRequest;
import com.hms.billing.dto.BillingDtos.PackageTemplateView;
import com.hms.billing.dto.BillingDtos.ServiceRequest;
import com.hms.billing.dto.BillingDtos.ServiceView;
import com.hms.billing.engine.InvoiceCalculator;
import com.hms.billing.entity.PackageTemplate;
import com.hms.billing.entity.ServiceItem;
import com.hms.billing.enums.ServiceCategory;
import com.hms.billing.repository.PackageTemplateRepository;
import com.hms.billing.repository.ServiceItemRepository;
import com.hms.common.audit.AuditPublisher;
import com.hms.common.exception.BusinessException;
import com.hms.common.exception.DuplicateResourceException;
import com.hms.common.exception.ResourceNotFoundException;
import com.hms.common.security.ClinicScope;
import com.hms.common.security.SecurityUtils;

import lombok.RequiredArgsConstructor;

/** The clinic's fee list and session packages. Price changes are audited. */
@Service
@RequiredArgsConstructor
public class CatalogService {

	private final ServiceItemRepository services;
	private final PackageTemplateRepository templates;
	private final AuditPublisher audit;

	// ------------------------------------------------------ services
	@Transactional(readOnly = true)
	public List<ServiceView> services(boolean activeOnly) {
		return services.findByClinicIdOrderBySortOrderAscNameAsc(ClinicScope.currentClinicId()).stream()
				.filter(s -> !activeOnly || s.isActive()).map(this::view).toList();
	}

	@Transactional
	public ServiceView createService(ServiceRequest r) {
		UUID clinic = ClinicScope.currentClinicId();
		String code = r.code().trim().toUpperCase();
		if (services.findByClinicIdAndCodeIgnoreCase(clinic, code).isPresent()) {
			throw new DuplicateResourceException("A service with code " + code + " already exists");
		}
		ServiceItem s = new ServiceItem();
		s.setClinicId(clinic);
		s.setCode(code);
		apply(s, r, clinic);
		ServiceItem saved = services.save(s);
		audit.record("BILLING_SERVICE_CREATED", "ServiceItem", saved.getId(),
				Map.of("code", code, "price", saved.getPrice().toPlainString()));
		return view(saved);
	}

	@Transactional
	public ServiceView updateService(UUID id, ServiceRequest r) {
		ServiceItem s = requireService(id, false);
		BigDecimal before = s.getPrice();
		apply(s, r, s.getClinicId());
		Map<String, Object> d = new LinkedHashMap<>();
		d.put("code", s.getCode());
		if (before.compareTo(s.getPrice()) != 0) {
			d.put("oldPrice", before.toPlainString());
			d.put("newPrice", s.getPrice().toPlainString());
		}
		audit.record("BILLING_SERVICE_UPDATED", "ServiceItem", id, d);
		return view(services.save(s));
	}

	@Transactional
	public void deactivateService(UUID id) {
		ServiceItem s = requireService(id, false);
		s.setActive(false);
		s.softDelete(SecurityUtils.currentUsername());
		audit.record("BILLING_SERVICE_REMOVED", "ServiceItem", id, Map.of("code", s.getCode()));
	}

	/**
	 * The service charged for completed visits of this type, if the clinic has set
	 * one up.
	 */
	@Transactional(readOnly = true)
	public Optional<ServiceItem> findForVisitType(UUID clinicId, String visitTypeCode) {
		if (visitTypeCode == null || visitTypeCode.isBlank())
			return Optional.empty();
		return services.findByClinicIdAndVisitTypeCodeIgnoreCaseAndActiveTrue(clinicId, visitTypeCode).stream()
				.findFirst();
	}

	@Transactional(readOnly = true)
	public ServiceItem requireService(UUID id, boolean mustBeActive) {
		ServiceItem s = services.findById(id).orElseThrow(() -> new ResourceNotFoundException("Service", id));
		ClinicScope.assertSameClinic(s.getClinicId(), "Service", id);
		if (mustBeActive && !s.isActive())
			throw new BusinessException("The service '" + s.getName() + "' is no longer offered");
		return s;
	}

	private void apply(ServiceItem s, ServiceRequest r, UUID clinic) {
		BigDecimal price = InvoiceCalculator.round2(r.price());
		if (price.signum() < 0 || price.compareTo(InvoiceCalculator.MAX_PRICE) > 0)
			throw new BusinessException("The price must be between 0 and 1,00,00,000");
		BigDecimal tax = r.taxRatePercent() == null ? BigDecimal.ZERO.setScale(2)
				: InvoiceCalculator.round2(r.taxRatePercent());
		if (tax.signum() < 0 || tax.compareTo(InvoiceCalculator.MAX_TAX) > 0)
			throw new BusinessException("The tax rate must be between 0 and 40 percent");
		String visitType = r.visitTypeCode() == null || r.visitTypeCode().isBlank() ? null
				: r.visitTypeCode().trim().toUpperCase();
		if (visitType != null) {
			for (ServiceItem other : services.findByClinicIdAndVisitTypeCodeIgnoreCaseAndActiveTrue(clinic,
					visitType)) {
				if (!other.getId().equals(s.getId())) {
					throw new DuplicateResourceException(
							"Visit type " + visitType + " is already charged as '" + other.getName() + "'");
				}
			}
		}
		s.setName(r.name().trim());
		s.setCategory(r.category() == null ? ServiceCategory.THERAPY : r.category());
		s.setPrice(price);
		s.setTaxRatePercent(tax);
		s.setVisitTypeCode(visitType);
		if (r.sortOrder() != null)
			s.setSortOrder(r.sortOrder());
		if (r.active() != null)
			s.setActive(r.active());
	}

	private ServiceView view(ServiceItem s) {
		return new ServiceView(s.getId(), s.getCode(), s.getName(), s.getCategory(), s.getPrice(),
				s.getTaxRatePercent(), s.getVisitTypeCode(), s.isActive(), s.getSortOrder());
	}

	// ------------------------------------------------------ packages
	@Transactional(readOnly = true)
	public List<PackageTemplateView> packages(boolean activeOnly) {
		return templates.findByClinicIdOrderByNameAsc(ClinicScope.currentClinicId()).stream()
				.filter(t -> !activeOnly || t.isActive()).map(this::view).toList();
	}

	@Transactional
	public PackageTemplateView createPackage(PackageTemplateRequest r) {
		PackageTemplate t = new PackageTemplate();
		t.setClinicId(ClinicScope.currentClinicId());
		apply(t, r);
		PackageTemplate saved = templates.save(t);
		audit.record("BILLING_PACKAGE_CREATED", "PackageTemplate", saved.getId(),
				Map.of("price", saved.getPrice().toPlainString()));
		return view(saved);
	}

	@Transactional
	public PackageTemplateView updatePackage(UUID id, PackageTemplateRequest r) {
		PackageTemplate t = requireTemplate(id, false);
		BigDecimal before = t.getPrice();
		apply(t, r);
		Map<String, Object> d = new LinkedHashMap<>();
		if (before.compareTo(t.getPrice()) != 0) {
			d.put("oldPrice", before.toPlainString());
			d.put("newPrice", t.getPrice().toPlainString());
		}
		audit.record("BILLING_PACKAGE_UPDATED", "PackageTemplate", id, d);
		return view(templates.save(t));
	}

	@Transactional
	public void deactivatePackage(UUID id) {
		PackageTemplate t = requireTemplate(id, false);
		t.setActive(false);
		t.softDelete(SecurityUtils.currentUsername());
	}

	@Transactional(readOnly = true)
	public PackageTemplate requireTemplate(UUID id, boolean mustBeActive) {
		PackageTemplate t = templates.findById(id).orElseThrow(() -> new ResourceNotFoundException("Package", id));
		ClinicScope.assertSameClinic(t.getClinicId(), "Package", id);
		if (mustBeActive && !t.isActive())
			throw new BusinessException("The package '" + t.getName() + "' is no longer sold");
		return t;
	}

	private void apply(PackageTemplate t, PackageTemplateRequest r) {
		ServiceItem svc = requireService(r.serviceId(), true);
		if (r.sessionCount() < 1 || r.sessionCount() > 200)
			throw new BusinessException("A package has 1 to 200 sessions");
		if (r.validityDays() < 1 || r.validityDays() > 1095)
			throw new BusinessException("Validity must be 1 to 1095 days");
		BigDecimal price = InvoiceCalculator.round2(r.price());
		if (price.signum() < 0 || price.compareTo(InvoiceCalculator.MAX_PRICE) > 0)
			throw new BusinessException("The price must be between 0 and 1,00,00,000");
		t.setName(r.name().trim());
		t.setServiceId(svc.getId());
		t.setSessionCount(r.sessionCount());
		t.setPrice(price);
		t.setValidityDays(r.validityDays());
		if (r.active() != null)
			t.setActive(r.active());
	}

	private PackageTemplateView view(PackageTemplate t) {
		String serviceName = services.findById(t.getServiceId()).map(ServiceItem::getName).orElse(null);
		return new PackageTemplateView(t.getId(), t.getName(), t.getServiceId(), serviceName, t.getSessionCount(),
				t.getPrice(), t.getValidityDays(), t.isActive());
	}
}