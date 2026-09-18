package com.my.craft.auditlog.service;

import java.util.List;

import com.my.craft.auditlog.dto.AuditLogEntryResponse;

/**
 * Reads the change history {@link AuditActionRecorder} wrote. Deliberately generic over the
 * entity type (a plain {@code Class<?>}, supplied by the caller) rather than naming any specific
 * domain class, since this module has no dependency on any {@code modules/*} library and
 * shouldn't gain one just to know about e.g. {@code com.my.craft.security.domain.User}.
 */
public interface AuditQueryService {

    /** Every recorded commit for one entity instance, newest first -- its full property state as
     * of each commit (creation, every update, and removal if it's been deleted), AND the state
     * immediately before each one ({@code AuditLogEntryResponse.before}, {@code null} only for
     * the very first commit) -- the only query here that populates {@code before}, since it's the
     * one query scoped to a single entity's own timeline. */
    List<AuditLogEntryResponse> findHistory(Class<?> entityType, Object entityId);

    /** The most recent commits across every audited entity, newest first, capped at {@code
     * limit} -- regardless of which {@link AuditActionRecorder#record} action produced them.
     * {@code AuditLogEntryResponse.before} is always {@code null} here (see {@link #findHistory}
     * for why); only {@code after} (the state as of that commit) is populated. */
    List<AuditLogEntryResponse> findRecent(int limit);

    /** The most recent commits under one specific marked action (e.g. {@code "USER_CREATED"} --
     * see {@link AuditActionRecorder}), newest first, capped at {@code limit}. Same {@code
     * before = null} caveat as {@link #findRecent}. */
    List<AuditLogEntryResponse> findByAction(String action, int limit);
}
