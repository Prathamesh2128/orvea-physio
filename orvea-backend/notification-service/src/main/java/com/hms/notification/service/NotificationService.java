package com.hms.notification.service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hms.common.api.PageResponse;
import com.hms.common.audit.AuditPublisher;
import com.hms.common.event.BaseEvent;
import com.hms.common.event.EventJson;
import com.hms.common.event.EventTypes;
import com.hms.common.exception.BusinessException;
import com.hms.common.exception.ResourceNotFoundException;
import com.hms.common.security.ClinicScope;
import com.hms.common.security.SecurityUtils;
import com.hms.notification.client.PatientConsentClient;
import com.hms.notification.dto.NotificationResponse;
import com.hms.notification.entity.MessageTemplate;
import com.hms.notification.entity.MessagingSettings;
import com.hms.notification.entity.Notification;
import com.hms.notification.enums.MessageType;
import com.hms.notification.enums.NotificationStatus;
import com.hms.notification.provider.ProviderException;
import com.hms.notification.provider.ProviderRegistry;
import com.hms.notification.provider.WhatsAppProvider;
import com.hms.notification.repository.NotificationRepository;
import com.hms.notification.util.PhoneNormalizer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

	static final int MAX_ATTEMPTS = 3;
	/**
	 * Everything a patient was told about an appointment that has since been
	 * changed or cancelled.
	 */
	private static final List<MessageType> WITHDRAWABLE = List.of(MessageType.BOOKING_CONFIRMATION,
			MessageType.REMINDER, MessageType.RESCHEDULE);

	private final NotificationRepository repository;
	private final MessagingSettingsService settingsService;
	private final TemplateService templateService;
	private final PatientConsentClient consentClient;
	private final ProviderRegistry registry;
	private final Clock clock;
	private final AuditPublisher audit;

	// =============================================== event handling
	@Transactional
	public void handle(BaseEvent e) {
		MessageType type = typeOf(e.getEventType());
		if (type == null)
			return; // not an event we message about

		if (type == MessageType.CANCELLATION)
			withdraw(e.uuid("appointmentId"));
		if (type == MessageType.RESCHEDULE)
			withdraw(e.uuid("rescheduledFromId"));

		String dedupeKey = switch (type) {
		case REMINDER -> "REMINDER:" + e.text("taskId");
		case MEASUREMENT_REMINDER -> "MEASUREMENT_REMINDER:" + e.text("planId") + ":" + e.text("dueDate");
		default -> type + ":" + e.getEventId();
		};

		if (repository.existsByDedupeKey(dedupeKey))
			return; // Kafka delivered it again

		MessagingSettings s = settingsService.effective(e.getClinicId());
		Notification n = new Notification();
		n.setClinicId(e.getClinicId());
		n.setDedupeKey(dedupeKey);
		n.setMessageType(type);
		n.setProvider(s.getProvider().name());
		n.setPatientId(e.uuid("patientId"));
		n.setAppointmentId(e.uuid("appointmentId"));
		LocalDateTime when = parse(e.text("appointmentAt"));
		if (when == null && e.text("dueDate") != null) {
			try {
				when = LocalDate.parse(e.text("dueDate")).atStartOfDay();
			} catch (RuntimeException ignored) {
				// leave empty
			}
		}
		n.setAppointmentAt(when);
		n.setRecipientName(e.text("patientName"));
		n.setRecipientPhone(PhoneNormalizer.normalize(e.text("patientPhone"), s.getDefaultCountryCode()));
		n.setStatus(NotificationStatus.SUPPRESSED);

		String reason = suppressionReason(s, type, n);
		if (reason != null) {
			n.setStatusReason(reason);
		} else {
			Map<String, String> vars = TemplateService.vars(n.getRecipientName(), n.getAppointmentAt(),
					e.text("providerName"), e.text("visitTypeName"), s.getClinicName(), s.getClinicPhone());
			MessageTemplate t = templateService.find(e.getClinicId(), type);
			n.setBody(TemplateService.fill(t.getBody(), vars));
			n.setTemplateVars(EventJson.write(vars));
			n.setStatus(registry.get(s.getProvider()).onCreate(n));
		}
		repository.save(n);
	}

	private String suppressionReason(MessagingSettings s, MessageType type, Notification n) {
		if (!s.isEnabled())
			return "WhatsApp messaging is switched off for this clinic";
		boolean typeOn = switch (type) {
		case BOOKING_CONFIRMATION -> s.isSendBookingConfirmation();
		case REMINDER, MEASUREMENT_REMINDER -> s.isSendReminders();
		case CANCELLATION -> s.isSendCancellation();
		case RESCHEDULE -> s.isSendReschedule();
		};
		if (!typeOn)
			return type.label() + " messages are switched off";
		if (n.getRecipientPhone() == null)
			return "No valid phone number";
		if (type == MessageType.REMINDER && n.getAppointmentAt() != null
				&& !n.getAppointmentAt().isAfter(LocalDateTime.now(clock))) {
			return "Appointment time has already passed";
		}
		// last, because it is a network call; a failure here throws so Kafka retries
		// (never drops silently)
		if (s.isRequireConsent() && !consentClient.hasWhatsAppConsent(n.getPatientId(), n.getClinicId())) {
			return "Patient has not consented to WhatsApp messages";
		}
		return null;
	}

	private void withdraw(UUID appointmentId) {
		if (appointmentId == null)
			return;
		repository.withdraw(appointmentId, WITHDRAWABLE, NotificationStatus.OPEN, NotificationStatus.CANCELLED,
				"Appointment was changed or cancelled");
	}

	private static MessageType typeOf(String eventType) {
		if (eventType == null)
			return null;
		return switch (eventType) {
		case EventTypes.APPOINTMENT_BOOKED -> MessageType.BOOKING_CONFIRMATION;
		case EventTypes.APPOINTMENT_RESCHEDULED -> MessageType.RESCHEDULE;
		case EventTypes.APPOINTMENT_CANCELLED -> MessageType.CANCELLATION;
		case EventTypes.REMINDER_DUE -> MessageType.REMINDER;
		case EventTypes.MEASUREMENT_DUE -> MessageType.MEASUREMENT_REMINDER;
		default -> null;
		};
	}

	private static LocalDateTime parse(String v) {
		try {
			return v == null ? null : LocalDateTime.parse(v);
		} catch (RuntimeException ex) {
			return null;
		}
	}

	// ====================================== automatic sending (CLOUD_API)
	/** Called by the dispatcher for each PENDING message. */
	@Transactional
	public void sendOne(UUID id) {
		if (repository.claim(id, NotificationStatus.PENDING, NotificationStatus.SENDING) == 0)
			return;
		Notification n = repository.findById(id).orElseThrow();
		MessagingSettings s = settingsService.effective(n.getClinicId());

		if (!s.isEnabled()) { // the clinic switch is honoured at send time too
			n.setStatus(NotificationStatus.SUPPRESSED);
			n.setStatusReason("WhatsApp messaging was switched off before this was sent");
			return;
		}
		WhatsAppProvider provider = registry.get(s.getProvider());
		NotificationStatus initial = provider.onCreate(n); // provider may have changed since it was queued
		if (initial != NotificationStatus.PENDING) {
			n.setStatus(initial);
			n.setProvider(s.getProvider().name());
			return;
		}
		try {
			n.setProviderMessageId(provider.send(n, s, templateService.find(n.getClinicId(), n.getMessageType())));
			n.setStatus(NotificationStatus.SENT);
			n.setSentAt(LocalDateTime.now(clock));
			n.setLastError(null);
		} catch (ProviderException ex) {
			n.setLastError(truncate(ex.getMessage()));
			n.setStatus(ex.isRetryable() && n.getAttempts() < MAX_ATTEMPTS ? NotificationStatus.PENDING
					: NotificationStatus.FAILED);
		}
	}

	// ============================================ outbox (staff screens)
	@Transactional(readOnly = true)
	public PageResponse<NotificationResponse> search(NotificationStatus status, MessageType type, Pageable pageable) {
		return PageResponse.from(repository.search(status, type, ClinicScope.currentClinicId(), pageable),
				NotificationService::toResponse);
	}

	@Transactional(readOnly = true)
	public NotificationResponse get(UUID id) {
		return toResponse(load(id));
	}

	/** Staff pressed Send in WhatsApp and confirms it. */
	@Transactional
	public NotificationResponse markSent(UUID id) {
		Notification n = load(id);
		if (n.getStatus() != NotificationStatus.READY_TO_SEND) {
			throw new BusinessException("Only messages waiting in the outbox can be marked as sent");
		}
		n.setStatus(NotificationStatus.SENT);
		n.setSentAt(LocalDateTime.now(clock));
		n.setSentBy(SecurityUtils.currentUsername());
		audit.record("NOTIFICATION_MARKED_SENT", "Notification", id, Map.of("type", n.getMessageType().name()));
		return toResponse(n);
	}

	@Transactional
	public NotificationResponse cancel(UUID id) {
		Notification n = load(id);
		if (n.getStatus() != NotificationStatus.READY_TO_SEND && n.getStatus() != NotificationStatus.PENDING
				&& n.getStatus() != NotificationStatus.FAILED) {
			throw new BusinessException("A " + n.getStatus() + " message cannot be cancelled");
		}
		n.setStatus(NotificationStatus.CANCELLED);
		n.setStatusReason("Cancelled by " + SecurityUtils.currentUsername());
		audit.record("NOTIFICATION_CANCELLED", "Notification", id, Map.of("type", n.getMessageType().name()));
		return toResponse(n);
	}

	@Transactional
	public NotificationResponse retry(UUID id) {
		Notification n = load(id);
		if (n.getStatus() != NotificationStatus.FAILED)
			throw new BusinessException("Only failed messages can be retried");
		MessagingSettings s = settingsService.effective(n.getClinicId());
		if (!s.isEnabled())
			throw new BusinessException("Switch WhatsApp messaging on before retrying");
		n.setAttempts(0);
		n.setLastError(null);
		n.setProvider(s.getProvider().name());
		n.setStatus(registry.get(s.getProvider()).onCreate(n));
		return toResponse(n);
	}

	private Notification load(UUID id) {
		Notification n = repository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Notification", id));
		ClinicScope.assertSameClinic(n.getClinicId(), "Notification", id);
		return n;
	}

	private static String truncate(String s) {
		String v = s == null ? "" : s;
		return v.substring(0, Math.min(255, v.length()));
	}

	static NotificationResponse toResponse(Notification n) {
		return new NotificationResponse(n.getId(), n.getMessageType(), n.getChannel(), n.getProvider(),
				n.getRecipientName(), n.getRecipientPhone(), n.getBody(), n.getStatus(), n.getStatusReason(),
				n.getWaLink(), n.getAppointmentId(), n.getAppointmentAt(), n.getAttempts(), n.getLastError(),
				n.getCreatedAt(), n.getSentAt(), n.getSentBy());
	}
}
