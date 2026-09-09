package com.my.craft.security.user;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

@Service
public class DefaultUserService implements UserService {

    @Override
    public UserContext currentUser(Authentication authentication) {
        Set<String> roles = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toUnmodifiableSet());

        return switch (authentication.getPrincipal()) {
            case OidcUser oidcUser -> new UserContext(
                    oidcUser.getSubject(),
                    oidcUser.getPreferredUsername(),
                    oidcUser.getEmail(),
                    roles,
                    Map.copyOf(oidcUser.getClaims()));
            case Jwt jwt -> new UserContext(
                    jwt.getSubject(),
                    jwt.getClaimAsString("preferred_username"),
                    jwt.getClaimAsString("email"),
                    roles,
                    Map.copyOf(jwt.getClaims()));
            case UserDetails userDetails -> new UserContext(
                    userDetails.getUsername(),
                    userDetails.getUsername(),
                    null,
                    roles,
                    Map.of());
            default -> new UserContext(authentication.getName(), authentication.getName(), null, roles, Map.of());
        };
    }
}
