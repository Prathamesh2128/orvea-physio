package com.hms.notification.provider;

@SuppressWarnings("serial")
public class ProviderException extends RuntimeException {
	private final boolean retryable;

	public ProviderException(String message, boolean retryable) {
		super(message);
		this.retryable = retryable;
	}

	public boolean isRetryable() {
		return retryable;
	}
}