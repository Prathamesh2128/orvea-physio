package com.hms.notification.service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hms.common.exception.BusinessException;
import com.hms.common.security.ClinicScope;
import com.hms.notification.dto.TemplateRequest;
import com.hms.notification.dto.TemplateResponse;
import com.hms.notification.entity.MessageTemplate;
import com.hms.notification.enums.MessageType;
import com.hms.notification.repository.MessageTemplateRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TemplateService {

	public static final Set<String> PLACEHOLDERS = Set.of("patientName", "date", "time", "doctor", "visit",
			"clinicName", "clinicPhone");

	/** Messages never contain diagnosis or treatment details. */
	static final Map<MessageType, String> DEFAULT_BODIES = Map.of(MessageType.BOOKING_CONFIRMATION,
			"Hello {patientName}, your appointment at {clinicName} is confirmed for {date} at {time} with {doctor} ({visit}). To change or cancel, please call {clinicPhone}.",
			MessageType.REMINDER,
			"Reminder: {patientName}, you have an appointment at {clinicName} on {date} at {time} with {doctor}. Please arrive 10 minutes early. To change it, call {clinicPhone}.",
			MessageType.CANCELLATION,
			"Hello {patientName}, your appointment at {clinicName} on {date} at {time} with {doctor} has been cancelled. Please call {clinicPhone} to book a new time.",
			MessageType.RESCHEDULE,
			"Hello {patientName}, your appointment at {clinicName} has been moved to {date} at {time} with {doctor}. If this time does not suit you, call {clinicPhone}.",
			MessageType.MEASUREMENT_REMINDER,
			"Hello {patientName}, your next measurement at {clinicName} is due on {date}. Please call {clinicPhone} to book a visit.");

	private static final Pattern TOKEN = Pattern.compile("\\{([A-Za-z]+)}");
	private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("EEE, dd MMM yyyy", Locale.ENGLISH);
	private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("hh:mm a", Locale.ENGLISH);

	private final MessageTemplateRepository repository;

	// ------------------------------------------------------ rendering
	public static String fill(String body, Map<String, String> vars) {
		Matcher m = TOKEN.matcher(body);
		StringBuilder out = new StringBuilder();
		while (m.find()) {
			m.appendReplacement(out, Matcher.quoteReplacement(vars.getOrDefault(m.group(1), "")));
		}
		m.appendTail(out);
		return out.toString().replaceAll("\\s+", " ").trim();
	}

	public static Map<String, String> vars(String patientName, LocalDateTime at, String doctor, String visit,
			String clinicName, String clinicPhone) {
		Map<String, String> v = new LinkedHashMap<>();
		v.put("patientName", patientName == null ? "" : patientName);
		v.put("date", at == null ? "" : at.format(DATE));
		v.put("time", at == null ? "" : at.format(TIME));
		v.put("doctor", doctor == null ? "" : doctor);
		v.put("visit", visit == null ? "" : visit);
		v.put("clinicName", clinicName == null ? "" : clinicName);
		v.put("clinicPhone", clinicPhone == null ? "" : clinicPhone);
		return v;
	}

	static void validatePlaceholders(String body) {
		Matcher m = TOKEN.matcher(body);
		while (m.find()) {
			if (!PLACEHOLDERS.contains(m.group(1))) {
				throw new BusinessException(
						"Unknown placeholder {" + m.group(1) + "}. Allowed: " + new TreeSet<>(PLACEHOLDERS));
			}
		}
	}

	// --------------------------------------------------------- lookup
	/**
	 * The clinic's active template, or the built-in default. Read-only, so safe
	 * inside consumers.
	 */
	@Transactional(readOnly = true)
	public MessageTemplate find(UUID clinicId, MessageType type) {
		return repository.findByClinicIdAndMessageType(clinicId, type).filter(MessageTemplate::isActive)
				.orElseGet(() -> defaultTemplate(clinicId, type));
	}

	@Transactional
	public List<TemplateResponse> list() {
		UUID clinic = ClinicScope.currentClinicId();
		for (MessageType t : MessageType.values()) {
			if (repository.findByClinicIdAndMessageType(clinic, t).isEmpty())
				repository.save(defaultTemplate(clinic, t));
		}
		return repository.findByClinicIdOrderByMessageTypeAsc(clinic).stream().map(this::toResponse).toList();
	}

	@Transactional
	public TemplateResponse update(MessageType type, TemplateRequest r) {
		validatePlaceholders(r.body());
		UUID clinic = ClinicScope.currentClinicId();
		MessageTemplate t = repository.findByClinicIdAndMessageType(clinic, type)
				.orElseGet(() -> defaultTemplate(clinic, type));
		t.setBody(r.body().trim());
		t.setCloudTemplateName(
				r.cloudTemplateName() == null || r.cloudTemplateName().isBlank() ? null : r.cloudTemplateName().trim());
		if (r.cloudLanguageCode() != null && !r.cloudLanguageCode().isBlank())
			t.setCloudLanguageCode(r.cloudLanguageCode().trim());
		if (r.cloudParamOrder() != null && !r.cloudParamOrder().isBlank()) {
			for (String k : r.cloudParamOrder().split(",")) {
				if (!PLACEHOLDERS.contains(k.trim()))
					throw new BusinessException("Unknown parameter: " + k.trim());
			}
			t.setCloudParamOrder(r.cloudParamOrder().replaceAll("\\s", ""));
		}
		if (r.active() != null)
			t.setActive(r.active());
		return toResponse(repository.save(t));
	}

	private MessageTemplate defaultTemplate(UUID clinicId, MessageType type) {
		MessageTemplate t = new MessageTemplate();
		t.setClinicId(clinicId);
		t.setMessageType(type);
		t.setBody(DEFAULT_BODIES.get(type));
		return t;
	}

	private TemplateResponse toResponse(MessageTemplate t) {
		return new TemplateResponse(t.getId(), t.getMessageType(), t.getBody(), t.getCloudTemplateName(),
				t.getCloudLanguageCode(), t.getCloudParamOrder(), t.isActive());
	}
}
