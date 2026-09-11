package com.my.craft.security.service;

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
     * login.
     */
    void initMasterAccount();
}
