package com.hms.notification.provider;

import org.springframework.stereotype.Component;

import com.hms.notification.entity.Notification;
import com.hms.notification.enums.NotificationStatus;
import com.hms.notification.enums.WhatsAppProviderType;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class LogOnlyProvider implements WhatsAppProvider {

	@Override
	public WhatsAppProviderType type() {
		return WhatsAppProviderType.LOG_ONLY;
	}

	@Override
	public NotificationStatus onCreate(Notification n) {
		String p = n.getRecipientPhone();
		log.info("[WHATSAPP-LOG-ONLY] {} to ******{} : {}", n.getMessageType(),
				p == null || p.length() < 4 ? "" : p.substring(p.length() - 4), n.getBody());
		return NotificationStatus.LOGGED;
	}
}
