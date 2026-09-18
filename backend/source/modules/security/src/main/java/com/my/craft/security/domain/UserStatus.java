package com.my.craft.security.domain;

/**
 * {@code User.status} -- tracks the async, outbox-driven sync to the identity provider (see
 * {@code com.my.craft.security.service.outbox.OutboxWorker}).
 */
public enum UserStatus {

    /** Just written locally by {@code DefaultUserAdminService.create}; not yet synced. */
    PENDING,

    /** {@code keycloakId} is set -- either the provider created a new account, or an existing
     * one was adopted as the same person. */
    ACTIVE,

    /** The provider already had an account for this username that isn't confidently the same
     * person -- needs a human, see {@code CreateUserOutcome.Conflict}. */
    CONFLICT,

    /** The provider permanently rejected the account creation (a non-retryable 4xx) -- needs a
     * human, see {@code CreateUserOutcome.PermanentFailure}. */
    FAILED
}
