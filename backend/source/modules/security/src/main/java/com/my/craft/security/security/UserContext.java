package com.my.craft.security.security;

import java.util.Map;
import java.util.Set;

/**
 * Enriched, provider-agnostic view of the current logged-in user -- built by
 * {@link UserService} from whatever principal type authenticated the request
 * (OIDC session, JWT, or Basic) so callers don't need to know which one it was.
 */
public record UserContext(
        String userId,
        String username,
        String email,
        Set<String> roles,
        Map<String, Object> attributes) {
}
