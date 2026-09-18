package com.my.craft.security.service;

import java.util.Optional;

import com.my.craft.security.service.initial.MasterAccountInitializer;

/** Port onto whichever identity provider actually owns accounts (Keycloak here, {@link
 * KeycloakUserService}; Cognito or another provider would be a sibling implementation). */
public interface UserService {

    /**
     * Attempts to create {@code username}/{@code email} as a new account. Called ONLY by {@code
     * com.my.craft.security.service.outbox.OutboxWorker} -- never synchronously from the web
     * tier, since this might not succeed for a while (retries) or ever (CONFLICT/FAILED). Never
     * throws for a failure the caller is expected to handle: every outcome, including "couldn't
     * reach the provider" and "the provider rejected the payload," is a value -- see {@link
     * CreateUserOutcome}.
     */
    CreateUserOutcome tryCreateUser(String username, String email);

    /** The provider's id for the account named {@code username}, if one already exists --
     * used by {@code com.my.craft.security.service.outbox.UserReconciliationJob} to heal a
     * {@code PENDING} row whose account turns out to already exist upstream. */
    Optional<String> findKeycloakIdByUsername(String username);

    void updateUser();
    void deactivateUser();

    /**
     * Idempotent: ensures the account described by {@code app.master-account.*} exists in the
     * provider, with its password set and {@code realmRole} assigned -- called by {@link
     * MasterAccountInitializer} on every boot so a fresh environment always has one working admin
     * login. Returns the provider's own id for that account (so the caller can link a local
     * record to it), or {@code null} if the bootstrap is disabled ({@code
     * app.master-account.enabled=false}).
     */
    String initMasterAccount();
}
