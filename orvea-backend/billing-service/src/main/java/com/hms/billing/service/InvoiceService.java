package com.hms.billing.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hms.billing.client.PatientClient;
import com.hms.billing.client.PatientRef;
import com.hms.billing.dto.InvoiceDtos.CreateInvoiceRequest;
import com.hms.billing.dto.InvoiceDtos.InvoiceLineView;
import com.hms.billing.dto.InvoiceDtos.InvoiceResponse;
import com.hms.billing.dto.InvoiceDtos.InvoiceSummary;
import com.hms.billing.dto.InvoiceDtos.LineRequest;
import com.hms.billing.engine.InvoiceCalculator;
import com.hms.billing.engine.InvoiceCalculator.LineInput;
import com.hms.billing.engine.InvoiceCalculator.Totals;
import com.hms.billing.entity.BillingSettings;
import com.hms.billing.entity.Invoice;
import com.hms.billing.entity.InvoiceLine;
import com.hms.billing.entity.PackageTemplate;
import com.hms.billing.entity.ServiceItem;
import com.hms.billing.entity.VisitCharge;
import com.hms.billing.enums.ChargeStatus;
import com.hms.billing.enums.InvoiceStatus;
import com.hms.billing.enums.LineType;
import com.hms.billing.repository.InvoiceRepository;
import com.hms.billing.repository.VisitChargeRepository;
import com.hms.common.api.PageResponse;
import com.hms.common.audit.AuditPublisher;
import com.hms.common.exception.BusinessException;
import com.hms.common.exception.ResourceNotFoundException;
import com.hms.common.security.ClinicScope;
import com.hms.common.security.SecurityUtils;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class InvoiceService {

	private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.ENGLISH);
	private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2);

	private final InvoiceRepository invoices;
	private final CatalogService catalog;
	private final VisitChargeRepository charges;
	private final PackageService packageService;
	private final NumberingService numbering;
	private final BillingSettingsService settingsService;
	private final PatientClient patientClient;
	private final PaymentQueries paymentQueries;
	private final AuditPublisher audit;
	private final Clock clock;

	// ===================================================== create
	@Transactional
	public InvoiceResponse create(CreateInvoiceRequest r) {
		PatientRef patient = patientClient.getPatient(r.patientId());
		if (!patient.active())
			throw new BusinessException("Patient record is inactive");
		UUID clinic = ClinicScope.currentClinicId();
		BillingSettings s = settingsService.effective(clinic);
		LocalDate today = LocalDate.now(clock);

		Invoice inv = new Invoice();
		inv.setClinicId(clinic);
		inv.setPatientId(patient.id());
		inv.setPatientName(patient.fullName());
		inv.setPatientUhid(patient.uhid());
		inv.setPatientPhone(patient.phone());
		inv.setCaseId(r.caseId());
		inv.setStatus(InvoiceStatus.DRAFT);
		inv.setInvoiceDate(today); // never back-dated
		inv.setNotes(r.notes());
		if (r.dueDate() != null) {
			if (r.dueDate().isBefore(today))
				throw new BusinessException("The due date cannot be in the past");
			inv.setDueDate(r.dueDate());
		} else if (s.getPaymentDueDays() > 0) {
			inv.setDueDate(today.plusDays(s.getPaymentDueDays()));
		}
		inv = invoices.save(inv);
		fill(inv, r.lines(), r.chargeIds(), s);
		if (Boolean.TRUE.equals(r.issue()))
			issueInternal(inv, s);
		return toResponse(invoices.save(inv), s);
	}

	/**
	 * A draft can be rebuilt freely. Visits linked before go back to the pending
	 * list first.
	 */
	@Transactional
	public InvoiceResponse update(UUID id, CreateInvoiceRequest r) {
		Invoice inv = load(id);
		requireStatus(inv, InvoiceStatus.DRAFT,
				"Only a draft can be edited. Cancel the invoice and create a new one instead.");
		BillingSettings s = settingsService.effective(inv.getClinicId());
		releaseCharges(inv.getId());
		String by = SecurityUtils.currentUsername();
		for (InvoiceLine l : new ArrayList<>(inv.getLines())) {
			l.softDelete(by);
			inv.getLines().remove(l);
		}
		inv.setNotes(r.notes());
		if (r.dueDate() != null) {
			if (r.dueDate().isBefore(LocalDate.now(clock)))
				throw new BusinessException("The due date cannot be in the past");
			inv.setDueDate(r.dueDate());
		}
		fill(inv, r.lines(), r.chargeIds(), s);
		if (Boolean.TRUE.equals(r.issue()))
			issueInternal(inv, s);
		return toResponse(invoices.save(inv), s);
	}

	private void fill(Invoice inv, List<LineRequest> lineRequests, List<UUID> chargeIds, BillingSettings s) {
		List<InvoiceLine> built = new ArrayList<>();
		List<LineInput> inputs = new ArrayList<>();
		List<VisitCharge> linked = new ArrayList<>();

		for (UUID chargeId : new LinkedHashSet<>(chargeIds == null ? List.<UUID>of() : chargeIds)) {
			VisitCharge c = charges.findById(chargeId)
					.orElseThrow(() -> new ResourceNotFoundException("Charge", chargeId));
			ClinicScope.assertSameClinic(c.getClinicId(), "Charge", chargeId);
			if (!c.getPatientId().equals(inv.getPatientId()))
				throw new BusinessException("A visit in the list belongs to a different patient");
			if (c.getStatus() != ChargeStatus.PENDING)
				throw new BusinessException("A visit in the list is already " + c.getStatus() + " and cannot be added");
			InvoiceLine l = newLine(inv, built.size() + 1, LineType.SERVICE, c.getServiceName(),
					c.getServiceName() + " on " + c.getServiceDate().format(DATE)
							+ (c.getProviderName() == null ? "" : " with " + c.getProviderName()));
			l.setServiceId(c.getServiceId());
			l.setChargeId(c.getId());
			built.add(l);
			inputs.add(new LineInput(1, c.getUnitPrice(), ZERO, c.getTaxRatePercent()));
			linked.add(c);
		}

		for (LineRequest lr : lineRequests == null ? List.<LineRequest>of() : lineRequests) {
			LineType type = lr.type() == null ? LineType.OTHER : lr.type();
			int qty = lr.quantity() == null ? 1 : lr.quantity();
			BigDecimal price;
			BigDecimal tax;
			InvoiceLine l;
			switch (type) {
			case SERVICE -> {
				if (lr.serviceId() == null)
					throw new BusinessException("A service line needs a service");
				ServiceItem svc = catalog.requireService(lr.serviceId(), true);
				price = priceOf(lr.unitPrice(), svc.getPrice(), svc.getName());
				tax = svc.getTaxRatePercent();
				l = newLine(inv, built.size() + 1, type, svc.getName(),
						blank(lr.description()) ? svc.getName() : lr.description().trim());
				l.setServiceId(svc.getId());
			}
			case PACKAGE -> {
				if (lr.packageTemplateId() == null)
					throw new BusinessException("A package line needs a package");
				if (qty != 1)
					throw new BusinessException("Sell one package per line");
				PackageTemplate t = catalog.requireTemplate(lr.packageTemplateId(), true);
				ServiceItem svc = catalog.requireService(t.getServiceId(), false);
				price = priceOf(lr.unitPrice(), t.getPrice(), t.getName());
				tax = svc.getTaxRatePercent();
				l = newLine(inv, built.size() + 1, type, t.getName(), "Package: " + t.getName() + " ("
						+ t.getSessionCount() + " sessions, valid " + t.getValidityDays() + " days)");
				l.setPackageTemplateId(t.getId());
			}
			default -> {
				if (blank(lr.description()))
					throw new BusinessException("A free item needs a description");
				if (lr.unitPrice() == null)
					throw new BusinessException("A free item needs a price");
				price = InvoiceCalculator.round2(lr.unitPrice());
				tax = lr.taxRatePercent() == null ? ZERO : InvoiceCalculator.round2(lr.taxRatePercent());
				String name = lr.description().trim();
				l = newLine(inv, built.size() + 1, type, name.length() > 120 ? name.substring(0, 120) : name, name);
			}
			}
			built.add(l);
			inputs.add(new LineInput(qty, price, lr.discountAmount(), tax));
		}
		if (built.isEmpty())
			throw new BusinessException("Add at least one item or visit to the invoice");

		Totals t = InvoiceCalculator.compute(inputs, s.isRoundOffToRupee());
		checkDiscount(t, s);
		for (int i = 0; i < built.size(); i++) {
			InvoiceLine l = built.get(i);
			LineInput in = inputs.get(i);
			InvoiceCalculator.LineResult res = t.lines().get(i);
			l.setQuantity(in.quantity());
			l.setUnitPrice(InvoiceCalculator.round2(in.unitPrice()));
			l.setDiscountAmount(res.discount());
			l.setTaxRatePercent(in.taxRatePercent() == null ? ZERO : InvoiceCalculator.round2(in.taxRatePercent()));
			l.setTaxableAmount(res.taxable());
			l.setTaxAmount(res.tax());
			l.setLineTotal(res.total());
			inv.getLines().add(l);
		}
		inv.setSubtotal(t.subtotal());
		inv.setDiscountTotal(t.discountTotal());
		inv.setTaxableTotal(t.taxableTotal());
		inv.setTaxTotal(t.taxTotal());
		inv.setRoundOff(t.roundOff());
		inv.setGrandTotal(t.grandTotal());
		linked.forEach(c -> {
			c.setStatus(ChargeStatus.INVOICED);
			c.setInvoiceId(inv.getId());
		});
	}

	private InvoiceLine newLine(Invoice inv, int no, LineType type, String itemName, String description) {
		InvoiceLine l = new InvoiceLine();
		l.setClinicId(inv.getClinicId());
		l.setInvoice(inv);
		l.setLineNo(no);
		l.setType(type);
		l.setItemName(itemName);
		l.setDescription(description.length() > 255 ? description.substring(0, 255) : description);
		return l;
	}

	/** The catalogue price unless a manager says otherwise. */
	private BigDecimal priceOf(BigDecimal given, BigDecimal catalogPrice, String what) {
		if (given == null)
			return catalogPrice;
		BigDecimal g = InvoiceCalculator.round2(given);
		if (g.compareTo(catalogPrice) != 0 && !can("BILLING_UPDATE")) {
			throw new BusinessException("Changing the price of '" + what + "' needs manager approval");
		}
		return g;
	}

	private void checkDiscount(Totals t, BillingSettings s) {
		if (t.discountTotal().signum() <= 0 || t.subtotal().signum() <= 0)
			return;
		BigDecimal pct = t.discountTotal().multiply(BigDecimal.valueOf(100)).divide(t.subtotal(), 2,
				RoundingMode.HALF_UP);
		if (pct.compareTo(s.getDiscountApprovalPercent()) > 0 && !can("BILLING_UPDATE")) {
			throw new BusinessException("A discount of " + pct + "% is above " + s.getDiscountApprovalPercent()
					+ "% and needs manager approval");
		}
	}

	private static boolean can(String permission) {
		return SecurityUtils.currentUser().map(u -> u.permissions().contains(permission)).orElse(false);
	}

	private static boolean blank(String v) {
		return v == null || v.isBlank();
	}

	// ===================================================== issue / cancel / delete
	@Transactional
	public InvoiceResponse issue(UUID id) {
		Invoice inv = load(id);
		BillingSettings s = settingsService.effective(inv.getClinicId());
		issueInternal(inv, s);
		return toResponse(invoices.save(inv), s);
	}

	private void issueInternal(Invoice inv, BillingSettings s) {
		requireStatus(inv, InvoiceStatus.DRAFT, "Only a draft can be issued");
		if (inv.getLines().isEmpty())
			throw new BusinessException("An invoice without items cannot be issued");
		inv.setInvoiceNumber(
				numbering.next(s.getInvoicePrefix(), inv.getClinicId(), inv.getInvoiceDate(), s.getFyStartMonth()));
		for (InvoiceLine l : inv.getLines()) {
			if (l.getType() == LineType.PACKAGE) {
				packageService.createFromSale(inv, l, catalog.requireTemplate(l.getPackageTemplateId(), false));
			}
		}
		inv.setStatus(inv.getGrandTotal().signum() == 0 ? InvoiceStatus.PAID : InvoiceStatus.ISSUED);
		inv.setIssuedAt(LocalDateTime.now(clock));
		inv.setIssuedBy(SecurityUtils.currentUsername());
		audit.record("INVOICE_ISSUED", "Invoice", inv.getId(),
				Map.of("number", inv.getInvoiceNumber(), "total", inv.getGrandTotal().toPlainString()));
	}

	/**
	 * An issued invoice is never edited. If it was wrong it is cancelled (money
	 * must be refunded first) and re-created.
	 */
	@Transactional
	public InvoiceResponse cancel(UUID id, String reason) {
		Invoice inv = load(id);
		if (inv.getStatus() == InvoiceStatus.DRAFT)
			throw new BusinessException("A draft is deleted, not cancelled");
		if (inv.getStatus() == InvoiceStatus.CANCELLED)
			throw new BusinessException("This invoice is already cancelled");
		if (blank(reason))
			throw new BusinessException("A reason is required to cancel an invoice");
		if (inv.getPaidAmount().signum() != 0) {
			throw new BusinessException("Payments of " + inv.getPaidAmount().toPlainString()
					+ " are still recorded on this invoice. Refund them first.");
		}
		packageService.cancelForInvoice(inv.getId(), "Invoice " + inv.getInvoiceNumber() + " cancelled");
		releaseCharges(inv.getId());
		inv.setStatus(InvoiceStatus.CANCELLED);
		inv.setCancelReason(reason.trim());
		inv.setCancelledAt(LocalDateTime.now(clock));
		audit.record("INVOICE_CANCELLED", "Invoice", id,
				Map.of("number", String.valueOf(inv.getInvoiceNumber()), "reason", reason.trim()));
		return toResponse(invoices.save(inv), settingsService.effective(inv.getClinicId()));
	}

	@Transactional
	public void deleteDraft(UUID id) {
		Invoice inv = load(id);
		requireStatus(inv, InvoiceStatus.DRAFT, "Only a draft can be deleted. An issued invoice is cancelled instead.");
		releaseCharges(id);
		String by = SecurityUtils.currentUsername();
		LocalDateTime at = LocalDateTime.now(clock);
		inv.getLines().forEach(l -> l.softDelete(by, at));
		inv.softDelete(by, at);
		audit.record("INVOICE_DRAFT_DELETED", "Invoice", id, Map.of());
	}

	/** Visits that were on this invoice become billable again. */
	private void releaseCharges(UUID invoiceId) {
		for (VisitCharge c : charges.findByInvoiceId(invoiceId)) {
			if (c.getStatus() == ChargeStatus.INVOICED) {
				c.setStatus(ChargeStatus.PENDING);
				c.setInvoiceId(null);
				charges.save(c);
			}
		}
	}

	// ===================================================== read
	@Transactional(readOnly = true)
	public InvoiceResponse get(UUID id) {
		Invoice inv = load(id);
		return toResponse(inv, settingsService.effective(inv.getClinicId()));
	}

	@Transactional(readOnly = true)
	public PageResponse<InvoiceSummary> search(UUID patientId, InvoiceStatus status, LocalDate from, LocalDate to,
			Pageable pageable) {
		return PageResponse.from(invoices.search(patientId, status, from, to, ClinicScope.currentClinicId(), pageable),
				i -> new InvoiceSummary(i.getId(), i.getInvoiceNumber(), i.getInvoiceDate(), i.getPatientName(),
						i.getPatientUhid(), i.getGrandTotal(), i.getPaidAmount(),
						i.getGrandTotal().subtract(i.getPaidAmount()), i.getStatus()));
	}

	private Invoice load(UUID id) {
		Invoice inv = invoices.findById(id).orElseThrow(() -> new ResourceNotFoundException("Invoice", id));
		ClinicScope.assertSameClinic(inv.getClinicId(), "Invoice", id);
		return inv;
	}

	private void requireStatus(Invoice inv, InvoiceStatus expected, String message) {
		if (inv.getStatus() != expected)
			throw new BusinessException(message);
	}

	InvoiceResponse toResponse(Invoice i, BillingSettings s) {
		List<InvoiceLineView> lines = i.getLines().stream().sorted(Comparator.comparingInt(InvoiceLine::getLineNo))
				.map(l -> new InvoiceLineView(l.getLineNo(), l.getType(), l.getItemName(), l.getDescription(),
						l.getQuantity(), l.getUnitPrice(), l.getDiscountAmount(), l.getTaxRatePercent(),
						l.getTaxableAmount(), l.getTaxAmount(), l.getLineTotal(), l.getServiceId(),
						l.getPackageTemplateId(), l.getChargeId()))
				.toList();
		return new InvoiceResponse(i.getId(), i.getInvoiceNumber(), i.getStatus(), i.getInvoiceDate(), i.getDueDate(),
				i.getPatientId(), i.getPatientName(), i.getPatientUhid(), i.getPatientPhone(), i.getCaseId(), lines,
				i.getSubtotal(), i.getDiscountTotal(), i.getTaxableTotal(), i.getTaxTotal(), i.getRoundOff(),
				i.getGrandTotal(), i.getPaidAmount(), i.getGrandTotal().subtract(i.getPaidAmount()),
				paymentQueries.payments(i.getId()), paymentQueries.refunds(i.getId()), i.getNotes(),
				settingsService.header(s), i.getIssuedAt(), i.getIssuedBy(), i.getCancelReason(), i.getCreatedAt());
	}
}