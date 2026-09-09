package com.my.craft.security.user;

import java.io.IOException;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Runs once per request, after whichever auth mechanism (OIDC login, JWT, Basic) has populated
 * the SecurityContext, and replaces that Authentication with a {@link CustomAuthenticationToken}
 * carrying the enriched {@link UserContext} from {@link UserService}. Registered on every
 * SecurityFilterChain -- see SecurityConfig.
 */
public class UserContextEnrichmentFilter extends OncePerRequestFilter {

    private final UserService userService;

    public UserContextEnrichmentFilter(UserService userService) {
        this.userService = userService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null
                && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken)
                && !(authentication instanceof CustomAuthenticationToken)) {
            UserContext userContext = userService.currentUser(authentication);
            SecurityContextHolder.getContext().setAuthentication(new CustomAuthenticationToken(authentication, userContext));
        }
        filterChain.doFilter(request, response);
    }
}
