package com.my.craft.auditlog.service;

/**
 * The "marking" primitive for audit log -- call this explicitly at the exact point a business
 * action happens, instead of relying on blanket per-repository auto-audit (that's what {@code
 * @JaversSpringDataAuditable} does, and is deliberately NOT used for entities audited this way
 * instead -- see backend/docs/audit-log.md). Only actions a caller actually records show up in
 * the audit log; nothing is captured automatically.
 *
 * <p>{@code entity} must be the actual JPA-mapped domain object (not a DTO) -- JaVers snapshots
 * its real property state. Call this right after the entity's own repository {@code save()} (for
 * {@link #record}) so JaVers sees the just-persisted state.
 */
public interface AuditActionRecorder {

    /** Commits {@code entity}'s current state, tagged with {@code action} (e.g. {@code
     * "USER_CREATED"}, {@code "ROLE_ASSIGNED"}) -- queryable back via {@code
     * AuditQueryService.findByAction}. */
    void record(String action, Object entity);

    /** Same as {@link #record}, but for an entity that's about to be (or just was) deleted --
     * commits a {@code TERMINAL} snapshot instead of a normal one ({@code
     * Javers.commitShallowDelete}), so the audit log has a record of its last state rather than
     * silently losing track of it. */
    void recordDeletion(String action, Object entity);
}
