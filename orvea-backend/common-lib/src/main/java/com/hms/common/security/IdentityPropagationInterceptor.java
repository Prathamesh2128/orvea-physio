package com.hms.common.security;

import java.io.IOException;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

import com.hms.common.constants.AppConstants;

// Forwards the caller's identity to another internal service (services trust these headers)
public class IdentityPropagationInterceptor implements ClientHttpRequestInterceptor {
	@Override
	public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution)
			throws IOException {
		SecurityUtils.currentUser().ifPresent(u -> {
			HttpHeaders h = request.getHeaders();
			h.set(AppConstants.HEADER_USER_ID, u.userId().toString());
			h.set(AppConstants.HEADER_USERNAME, u.username());
			h.set(AppConstants.HEADER_ROLES, String.join(",", u.roles()));
			h.set(AppConstants.HEADER_PERMISSIONS, String.join(",", u.permissions()));
			if (u.sessionId() != null)
				h.set(AppConstants.HEADER_SESSION_ID, u.sessionId());
			if (u.clinicId() != null)
				h.set(AppConstants.HEADER_CLINIC_ID, u.clinicId().toString());
		});
		return execution.execute(request, body);
	}
}
