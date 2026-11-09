package com.hms.billing.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hms.billing.dto.BillingDtos.SettingsRequest;
import com.hms.billing.dto.BillingDtos.SettingsResponse;
import com.hms.billing.dto.InvoiceDtos.ClinicHeader;
import com.hms.billing.entity.BillingSettings;
import com.hms.billing.repository.BillingSettingsRepository;
import com.hms.common.audit.AuditPublisher;
import com.hms.common.exception.BusinessException;
import com.hms.common.security.ClinicScope;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class BillingSettingsService {

	private static final Pattern PREFIX = Pattern.compile("^[A-Z0-9]{1,10}$");
	private static final Pattern GSTIN = Pattern.compile("^[0-9]{2}[A-Z]{5}[0-9]{4}[A-Z][1-9A-Z]Z[0-9A-Z]$");

	private final BillingSettingsRepository repository;
	private final AuditPublisher audit;

	@Transactional
	public BillingSettings effective(UUID clinicId) {
		return repository.findByClinicId(clinicId).orElseGet(() -> {
			BillingSettings s = new BillingSettings();
			s.setClinicId(clinicId);
			return repository.save(s);
		});
	}

	@Transactional
	public SettingsResponse get() {
		return toResponse(effective(ClinicScope.currentClinicId()));
	}

	@Transactional
	public SettingsResponse update(SettingsRequest r) {
		BillingSettings s = effective(ClinicScope.currentClinicId());
		Map<String, Object> changed = new LinkedHashMap<>();
		if (r.clinicName() != null) {
			s.setClinicName(blankToNull(r.clinicName()));
			changed.put("clinicName", true);
		}
		if (r.address() != null)
			s.setAddress(blankToNull(r.address()));
		if (r.phone() != null)
			s.setPhone(blankToNull(r.phone()));
		if (r.email() != null)
			s.setEmail(blankToNull(r.email()));
		if (r.registrationNo() != null)
			s.setRegistrationNo(blankToNull(r.registrationNo()));
		if (r.upiId() != null)
			s.setUpiId(blankToNull(r.upiId()));
		if (r.footerNote() != null)
			s.setFooterNote(blankToNull(r.footerNote()));
		if (r.gstin() != null) {
			String g = r.gstin().isBlank() ? null : r.gstin().trim().toUpperCase();
			if (g != null && !GSTIN.matcher(g).matches())
				throw new BusinessException("That GSTIN is not in the correct format");
			s.setGstin(g);
			changed.put("gstin", g != null);
		}
		if (r.invoicePrefix() != null) {
			s.setInvoicePrefix(prefix(r.invoicePrefix()));
			changed.put("invoicePrefix", s.getInvoicePrefix());
		}
		if (r.receiptPrefix() != null) {
			s.setReceiptPrefix(prefix(r.receiptPrefix()));
			changed.put("receiptPrefix", s.getReceiptPrefix());
		}
		if (r.fyStartMonth() != null) {
			if (r.fyStartMonth() < 1 || r.fyStartMonth() > 12)
				throw new BusinessException("The financial year start month must be 1 to 12");
			s.setFyStartMonth(r.fyStartMonth());
			changed.put("fyStartMonth", r.fyStartMonth());
		}
		if (r.roundOffToRupee() != null) {
			s.setRoundOffToRupee(r.roundOffToRupee());
			changed.put("roundOffToRupee", r.roundOffToRupee());
		}
		if (r.discountApprovalPercent() != null) {
			if (r.discountApprovalPercent().signum() < 0
					|| r.discountApprovalPercent().compareTo(new BigDecimal("100")) > 0) {
				throw new BusinessException("The discount approval level must be between 0 and 100 percent");
			}
			s.setDiscountApprovalPercent(r.discountApprovalPercent().setScale(2, RoundingMode.HALF_UP));
			changed.put("discountApprovalPercent", s.getDiscountApprovalPercent());
		}
		if (r.paymentDueDays() != null) {
			if (r.paymentDueDays() < 0 || r.paymentDueDays() > 365)
				throw new BusinessException("Payment due days must be 0 to 365");
			s.setPaymentDueDays(r.paymentDueDays());
			changed.put("paymentDueDays", r.paymentDueDays());
		}
		audit.record("BILLING_SETTINGS_CHANGED", "BillingSettings", s.getId(), changed);
		return toResponse(s);
	}

	public ClinicHeader header(BillingSettings s) {
		return new ClinicHeader(s.getClinicName(), s.getAddress(), s.getPhone(), s.getEmail(), s.getGstin(),
				s.getRegistrationNo(), s.getUpiId(), s.getFooterNote());
	}

	private static String prefix(String v) {
		String p = v.trim().toUpperCase();
		if (!PREFIX.matcher(p).matches())
			throw new BusinessException("A number prefix is 1 to 10 letters or digits");
		return p;
	}

	private static String blankToNull(String v) {
		return v == null || v.isBlank() ? null : v.trim();
	}

	private SettingsResponse toResponse(BillingSettings s) {
		return new SettingsResponse(s.getClinicName(), s.getAddress(), s.getPhone(), s.getEmail(), s.getGstin(),
				s.getRegistrationNo(), s.getUpiId(), s.getInvoicePrefix(), s.getReceiptPrefix(), s.getFyStartMonth(),
				s.isRoundOffToRupee(), s.getDiscountApprovalPercent(), s.getPaymentDueDays(), s.getFooterNote());
	}
}