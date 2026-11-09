package com.hms.notification.provider;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import com.hms.common.event.EventJson;
import com.hms.notification.entity.MessageTemplate;
import com.hms.notification.entity.MessagingSettings;
import com.hms.notification.entity.Notification;
import com.hms.notification.enums.NotificationStatus;
import com.hms.notification.enums.WhatsAppProviderType;
import com.hms.notification.security.SecretCipher;

/**
 * Meta WhatsApp Cloud API. Paid per delivered template message (utility rate).
 * Reminders need a template approved in Meta Business Manager; free text is not
 * allowed outside a 24h window.
 */
@Component
public class CloudApiProvider implements WhatsAppProvider {

	private final RestClient client;
	private final SecretCipher cipher;
	private final String version;

	public CloudApiProvider(RestClient.Builder builder, SecretCipher cipher,
			@Value("${physio.whatsapp.graph-base-url:https://graph.facebook.com}") String baseUrl,
			@Value("${physio.whatsapp.graph-version:v23.0}") String version) {
		this.client = builder.baseUrl(baseUrl).build();
		this.cipher = cipher;
		this.version = version;
	}

	@Override
	public WhatsAppProviderType type() {
		return WhatsAppProviderType.CLOUD_API;
	}

	@Override
	public NotificationStatus onCreate(Notification n) {
		return NotificationStatus.PENDING;
	}

	@Override
	@SuppressWarnings("unchecked")
	public String send(Notification n, MessagingSettings s, MessageTemplate t) {
		if (s.getCloudPhoneNumberId() == null || s.getCloudAccessTokenEnc() == null) {
			throw new ProviderException("WhatsApp Cloud API is not configured", false);
		}
		if (t == null || t.getCloudTemplateName() == null || t.getCloudTemplateName().isBlank()) {
			throw new ProviderException("No approved WhatsApp template is set for " + n.getMessageType(), false);
		}
		Map<String, String> vars = n.getTemplateVars() == null ? Map.of()
				: EventJson.read(n.getTemplateVars(), Map.class);
		List<Map<String, Object>> params = new ArrayList<>();
		for (String key : (t.getCloudParamOrder() == null ? "" : t.getCloudParamOrder()).split(",")) {
			if (key.isBlank())
				continue;
			String v = String.valueOf(vars.getOrDefault(key.trim(), "")).replaceAll("\\s+", " ").trim();
			params.add(Map.of("type", "text", "text", v.isEmpty() ? "-" : v)); // Meta rejects empty parameters
		}
		Map<String, Object> template = new LinkedHashMap<>();
		template.put("name", t.getCloudTemplateName());
		template.put("language", Map.of("code", t.getCloudLanguageCode() == null ? "en" : t.getCloudLanguageCode()));
		template.put("components", List.of(Map.of("type", "body", "parameters", params)));

		Map<String, Object> body = new LinkedHashMap<>();
		body.put("messaging_product", "whatsapp");
		body.put("to", n.getRecipientPhone());
		body.put("type", "template");
		body.put("template", template);

		try {
			Map<String, Object> resp = client.post()
					.uri("/{version}/{phoneId}/messages", version, s.getCloudPhoneNumberId())
					.header(HttpHeaders.AUTHORIZATION, "Bearer " + cipher.decrypt(s.getCloudAccessTokenEnc()))
					.contentType(MediaType.APPLICATION_JSON).body(body).retrieve().body(Map.class);
			Object messages = resp == null ? null : resp.get("messages");
			if (messages instanceof List<?> l && !l.isEmpty() && l.get(0) instanceof Map<?, ?> m
					&& m.get("id") != null) {
				return m.get("id").toString();
			}
			throw new ProviderException("WhatsApp accepted the request but returned no message id", true);
		} catch (HttpClientErrorException e) {
			boolean tooMany = e.getStatusCode().value() == 429;
			throw new ProviderException("WhatsApp rejected the message (" + e.getStatusCode().value() + "): "
					+ trim(e.getResponseBodyAsString()), tooMany);
		} catch (HttpServerErrorException | ResourceAccessException e) {
			throw new ProviderException("WhatsApp is temporarily unavailable: " + trim(e.getMessage()), true);
		}
	}

	private static String trim(String s) {
		String v = s == null ? "" : s;
		return v.substring(0, Math.min(180, v.length()));
	}
}
