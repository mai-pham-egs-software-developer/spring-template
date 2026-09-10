package com.my.craft.security.authz;

import java.util.function.Supplier;

import org.casbin.jcasbin.main.Enforcer;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;

import com.my.craft.security.user.CustomAuthenticationToken;

/**
 * Bridges {@code authorizeHttpRequests(...).access(...)} to the jcasbin {@link Enforcer}: the
 * enriched {@link com.my.craft.security.user.UserContext#username()} is the RBAC subject, the
 * request path is the object, and the HTTP method is the action. See rbac_model.conf /
 * rbac_policy.csv and docs/security.md. Requires
 * {@link com.my.craft.security.user.UserContextEnrichmentFilter} to have already run, i.e. must
 * only be used on chains that register it before this manager is checked.
 */
public class CasbinAuthorizationManager implements AuthorizationManager<RequestAuthorizationContext> {

    private final Enforcer enforcer;

    public CasbinAuthorizationManager(Enforcer enforcer) {
        this.enforcer = enforcer;
    }

    @Override
    public AuthorizationDecision check(Supplier<Authentication> authentication, RequestAuthorizationContext context) {
        Authentication auth = authentication.get();
        if (!(auth instanceof CustomAuthenticationToken token)) {
            return new AuthorizationDecision(false);
        }
        String subject = token.getUserContext().username();
        String object = context.getRequest().getRequestURI();
        String action = context.getRequest().getMethod();
        boolean granted = enforcer.enforce(subject, object, action);
        return new AuthorizationDecision(granted);
    }
}
