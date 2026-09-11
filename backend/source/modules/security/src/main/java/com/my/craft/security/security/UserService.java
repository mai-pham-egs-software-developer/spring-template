package com.my.craft.security.security;

import org.springframework.security.core.Authentication;

/**
 * Builds the enriched {@link UserContext} for whatever principal a
 * {@link org.springframework.security.core.Authentication} carries. This is the one place that
 * knows how to read each supported principal type (OidcUser, Jwt, UserDetails, ...) -- extend it
 * here (e.g. look up more profile data from modules/persistent) rather than in individual
 * controllers.
 */
public interface UserService {

    UserContext currentUser(Authentication authentication);
}
