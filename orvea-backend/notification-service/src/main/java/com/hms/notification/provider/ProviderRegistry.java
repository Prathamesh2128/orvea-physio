package com.hms.notification.provider;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.hms.notification.enums.WhatsAppProviderType;

@Component
public class ProviderRegistry {

	private final Map<WhatsAppProviderType, WhatsAppProvider> providers = new EnumMap<>(WhatsAppProviderType.class);

	public ProviderRegistry(List<WhatsAppProvider> all) {
		all.forEach(p -> providers.put(p.type(), p));
	}

	public WhatsAppProvider get(WhatsAppProviderType type) {
		WhatsAppProvider p = providers.get(type);
		if (p == null)
			throw new IllegalStateException("No WhatsApp provider registered for " + type);
		return p;
	}
}