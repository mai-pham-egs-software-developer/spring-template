package com.example.main.web;

import java.util.Map;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class MeController {

    // Principal is an OidcUser for browser session logins (oauth2Login) and a
    // Jwt for Bearer-token API calls (oauth2ResourceServer) -- see SecurityConfig.
    @GetMapping("/")
    public Map<String, Object> me(Authentication authentication) {
        return switch (authentication.getPrincipal()) {
            case OidcUser oidcUser -> oidcUser.getClaims();
            case Jwt jwt -> jwt.getClaims();
            default -> throw new IllegalStateException(
                    "Unsupported principal type: " + authentication.getPrincipal().getClass());
        };
    }
}
