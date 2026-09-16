package com.my.craft.security.service;

import com.my.craft.security.service.initial.MasterAccountInitializer;

/** Port onto whichever identity provider actually owns accounts (Keycloak here, {@link
 * KeycloakUserService}; Cognito or another provider would be a sibling implementation). */
public interface UserService {

    void createUser();
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
