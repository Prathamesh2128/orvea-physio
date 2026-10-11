package com.hms.common.security;

import java.util.Optional;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public final class SecurityUtils {
    private SecurityUtils() {}

    public static Optional<AuthenticatedUser> currentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof AuthenticatedUser u) {
            return Optional.of(u);
        }
        return Optional.empty();
    }

    public static String currentUsername() {
        return currentUser().map(AuthenticatedUser::username).orElse("SYSTEM");
    }
}
