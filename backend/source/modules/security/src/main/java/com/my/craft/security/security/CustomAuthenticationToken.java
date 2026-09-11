package com.my.craft.security.security;

import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.Authentication;

/**
 * Wraps whatever {@link Authentication} the active auth mechanism (OIDC session, JWT, Basic)
 * produced, adding the enriched {@link UserContext} alongside it. Principal/credentials/name are
 * delegated straight through so existing code reading {@code authentication.getPrincipal()} still
 * works unchanged -- callers that want the enriched data ask for {@link #getUserContext()}.
 */
public class CustomAuthenticationToken extends AbstractAuthenticationToken {

    private final Authentication delegate;
    private final UserContext userContext;

    public CustomAuthenticationToken(Authentication delegate, UserContext userContext) {
        super(delegate.getAuthorities());
        this.delegate = delegate;
        this.userContext = userContext;
        setDetails(delegate.getDetails());
        setAuthenticated(delegate.isAuthenticated());
    }

    @Override
    public Object getPrincipal() {
        return delegate.getPrincipal();
    }

    @Override
    public Object getCredentials() {
        return delegate.getCredentials();
    }

    @Override
    public String getName() {
        return delegate.getName();
    }

    public Authentication getDelegate() {
        return delegate;
    }

    public UserContext getUserContext() {
        return userContext;
    }
}
