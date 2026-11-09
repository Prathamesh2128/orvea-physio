package com.hms.notification.provider;

import org.springframework.stereotype.Component;

import com.hms.notification.entity.Notification;
import com.hms.notification.enums.NotificationStatus;
import com.hms.notification.enums.WhatsAppProviderType;
import com.hms.notification.util.WaLink;

/**
 * Free. Staff opens the prepared WhatsApp chat from the outbox and presses
 * Send.
 */
@Component
public class ManualLinkProvider implements WhatsAppProvider {

	@Override
	public WhatsAppProviderType type() {
		return WhatsAppProviderType.MANUAL_LINK;
	}

	@Override
	public NotificationStatus onCreate(Notification n) {
		n.setWaLink(WaLink.build(n.getRecipientPhone(), n.getBody()));
		return NotificationStatus.READY_TO_SEND;
	}
}
