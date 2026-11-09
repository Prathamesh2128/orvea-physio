package com.hms.billing.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import com.hms.billing.enums.ChargeStatus;
import com.hms.billing.enums.PackageStatus;
import com.hms.billing.enums.ServiceCategory;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public final class BillingDtos {
	private BillingDtos() {
	}

	/** Only what is sent is changed. */
	public record SettingsRequest(@Size(max = 120) String clinicName, @Size(max = 255) String address,
			@Size(max = 30) String phone, @Size(max = 100) String email, @Size(max = 20) String gstin,
			@Size(max = 60) String registrationNo, @Size(max = 80) String upiId, String invoicePrefix,
			String receiptPrefix, Integer fyStartMonth, Boolean roundOffToRupee, BigDecimal discountApprovalPercent,
			Integer paymentDueDays, @Size(max = 300) String footerNote) {
	}

	public record SettingsResponse(String clinicName, String address, String phone, String email, String gstin,
			String registrationNo, String upiId, String invoicePrefix, String receiptPrefix, int fyStartMonth,
			boolean roundOffToRupee, BigDecimal discountApprovalPercent, int paymentDueDays, String footerNote) {
	}

	public record ServiceRequest(
			@NotBlank @Pattern(regexp = "^[A-Za-z0-9_]{1,40}$", message = "Letters, digits and underscore only") String code,
			@NotBlank @Size(max = 120) String name, ServiceCategory category, @NotNull BigDecimal price,
			BigDecimal taxRatePercent, @Size(max = 40) String visitTypeCode, Integer sortOrder, Boolean active) {
	}

	public record ServiceView(UUID id, String code, String name, ServiceCategory category, BigDecimal price,
			BigDecimal taxRatePercent, String visitTypeCode, boolean active, int sortOrder) {
	}

	public record PackageTemplateRequest(@NotBlank @Size(max = 120) String name, @NotNull UUID serviceId,
			@NotNull Integer sessionCount, @NotNull BigDecimal price, @NotNull Integer validityDays, Boolean active) {
	}

	public record PackageTemplateView(UUID id, String name, UUID serviceId, String serviceName, int sessionCount,
			BigDecimal price, int validityDays, boolean active) {
	}

	public record ChargeView(UUID id, UUID appointmentId, UUID patientId, String patientName, String patientUhid,
			UUID caseId, String providerName, UUID serviceId, String serviceName, LocalDate serviceDate,
			BigDecimal unitPrice, BigDecimal taxRatePercent, ChargeStatus status, UUID packageId, UUID invoiceId,
			String note) {
	}

	public record ReasonRequest(@NotBlank @Size(max = 255) String reason) {
	}

	public record AssignServiceRequest(@NotNull UUID serviceId) {
	}

	public record CoverRequest(@NotNull UUID packageId) {
	}

	public record PackageView(UUID id, UUID patientId, String patientName, String name, UUID serviceId,
			String serviceName, int sessionsTotal, int sessionsUsed, int sessionsRemaining, LocalDate purchasedOn,
			LocalDate validUntil, PackageStatus status, UUID invoiceId, UUID caseId, BigDecimal price) {
	}

	public record UsageView(UUID id, UUID appointmentId, LocalDate usedOn, String source, String note, boolean reversed,
			String reverseReason) {
	}
}