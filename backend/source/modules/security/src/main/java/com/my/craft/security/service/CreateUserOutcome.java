package com.my.craft.security.service;

/**
 * Every way {@link UserService#tryCreateUser} can end, as a closed set of values instead of
 * exceptions -- {@code com.my.craft.security.service.outbox.OutboxWorker} switches on this to
 * decide what to do next: activate the local {@code User} row ({@link Created}/{@link Adopted}),
 * park it for a human ({@link Conflict}), or retry/dead-letter the outbox event ({@link
 * TransientFailure}/{@link PermanentFailure}). See {@code backend/docs/user-outbox.md}.
 */
public sealed interface CreateUserOutcome {

    /** The provider created a brand-new account; {@code keycloakId} is its id, already holding
     * the fresh temporary password {@code KeycloakUserService} just set on it. */
    record Created(String keycloakId) implements CreateUserOutcome {}

    /** The provider already had an account for this username (HTTP 409) that a follow-up lookup
     * judged to be the same person (matching email) -- linked as-is, its existing credentials
     * left untouched. */
    record Adopted(String keycloakId) implements CreateUserOutcome {}

    /** The provider already had an account for this username (HTTP 409) that isn't confidently
     * the same person -- or the follow-up lookup couldn't even find it (drift). Needs a human,
     * not an automatic retry. */
    record Conflict() implements CreateUserOutcome {}

    /** A retryable failure -- network error, timeout, or an HTTP 5xx from the provider. */
    record TransientFailure(String message) implements CreateUserOutcome {}

    /** A non-retryable failure -- the provider rejected the request itself (any 4xx other than
     * the 409 handled by {@link Conflict}), e.g. an invalid email. */
    record PermanentFailure(String message) implements CreateUserOutcome {}
}
