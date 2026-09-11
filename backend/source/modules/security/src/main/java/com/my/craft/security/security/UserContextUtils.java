package com.my.craft.security.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Reads the {@link UserContext} off whatever {@link Authentication} is on
 * {@link SecurityContextHolder} for the current thread -- every chain in {@code SecurityConfig}
 * registers {@link UserContextEnrichmentFilter} before authorization runs, so any authenticated
 * request has already been wrapped into a {@link CustomAuthenticationToken} by the time code here
 * runs.
 */
public final class UserContextUtils {

    private UserContextUtils() {}

    public static UserContext currentUserContext() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (!(authentication instanceof CustomAuthenticationToken customAuthentication)) {
            throw new IllegalStateException(
                    "Expected a CustomAuthenticationToken -- is UserContextEnrichmentFilter registered on this chain?");
        }
        return customAuthentication.getUserContext();
    }
}
