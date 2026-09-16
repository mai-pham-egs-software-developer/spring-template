package com.my.craft.security.service;

/** No {@code UserOrganizationRole} row exists for the given user within the given organization. */
public class OrganizationMemberNotFoundException extends RuntimeException {

    public OrganizationMemberNotFoundException(Long organizationId, String userId) {
        super("user " + userId + " is not a member of organization " + organizationId);
    }
}
