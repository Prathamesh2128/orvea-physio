package com.hms.notification.provider;

import com.hms.notification.entity.MessageTemplate;
import com.hms.notification.entity.MessagingSettings;
import com.hms.notification.entity.Notification;
import com.hms.notification.enums.NotificationStatus;
import com.hms.notification.enums.WhatsAppProviderType;

public interface WhatsAppProvider {

	WhatsAppProviderType type();

	/**
	 * Called when a message is created (or re-queued). Returns the status it starts
	 * in.
	 */
	NotificationStatus onCreate(Notification n);

	/** Automatic providers only. Returns the provider's message id. */
	default String send(Notification n, MessagingSettings settings, MessageTemplate template) {
		throw new UnsupportedOperationException(type() + " does not send automatically");
	}
}
