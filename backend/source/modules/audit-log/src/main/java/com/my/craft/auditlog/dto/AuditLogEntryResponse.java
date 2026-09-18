package com.my.craft.auditlog.dto;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * One JaVers commit for one entity instance -- its full property state right after that commit
 * ({@link #after}), and, where computed, its state right before ({@link #before}) -- API-facing
 * shape built from {@code org.javers.core.metamodel.object.CdoSnapshot}.
 *
 * @param globalId JaVers' own id for the audited object, e.g.
 *     {@code "com.my.craft.security.domain.User/e5a20aac-..."} -- qualified class name + local id.
 * @param snapshotType {@code CdoSnapshot.getType()}: {@code INITIAL} (first commit), {@code
 *     UPDATE}, or {@code TERMINAL} (removed as of this commit, via {@code
 *     AuditActionRecorder.recordDeletion}) -- JaVers' own low-level classification, independent
 *     of {@link #markedAction}.
 * @param markedAction the business action name passed to {@code AuditActionRecorder.record}/
 *     {@code recordDeletion} (e.g. {@code "USER_CREATED"}) -- {@code null} for a snapshot
 *     committed some other way (there shouldn't be any once every audited repository's blanket
 *     auto-audit is replaced by explicit recorder calls, but nothing enforces that at the JaVers
 *     level, so this stays nullable rather than assumed).
 * @param author whoever JaVers' configured {@code AuthorProvider} resolved for the commit --
 *     defaults to the current Spring Security {@code Authentication.getName()}.
 * @param committedAt when the commit happened.
 * @param commitId JaVers' own commit id, rendered as a string since its exact numeric shape is a
 *     JaVers implementation detail this DTO doesn't need to expose typed.
 * @param before every property name → value as of the commit immediately BEFORE this one, for
 *     the same entity instance -- {@code null} for the entity's very first commit ({@code
 *     snapshotType = INITIAL}, nothing came before it), and also {@code null} from {@code
 *     AuditQueryService.findRecent}/{@code findByAction} (see their javadoc -- computing this
 *     across a mixed set of entities is a separate query per row, not done for those). Only
 *     {@code AuditQueryService.findHistory} (one entity's full timeline) populates it.
 * @param after every property name → value as of THIS commit -- e.g. {@code {"name": "Alice",
 *     "email": "alice@example.com"}}. Always present, unlike {@link #before}.
 */
public record AuditLogEntryResponse(
        String globalId,
        String snapshotType,
        String markedAction,
        String author,
        LocalDateTime committedAt,
        String commitId,
        Map<String, Object> before,
        Map<String, Object> after) {}
