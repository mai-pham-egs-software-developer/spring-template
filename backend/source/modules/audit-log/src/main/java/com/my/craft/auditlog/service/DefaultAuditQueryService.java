package com.my.craft.auditlog.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.javers.core.Javers;
import org.javers.core.metamodel.object.CdoSnapshot;
import org.javers.core.metamodel.object.CdoSnapshotState;
import org.javers.repository.jql.JqlQuery;
import org.javers.repository.jql.QueryBuilder;
import org.springframework.stereotype.Service;

import com.my.craft.auditlog.dto.AuditLogEntryResponse;

/**
 * Delegates entirely to the {@link Javers} bean {@code javers-spring-boot-starter-sql}
 * auto-configures (backed by whatever {@code DataSource} the assembling application already has
 * -- see {@code backend/docs/audit-log.md}). Every method here just runs a JaVers query and maps
 * {@link CdoSnapshot}s to the wire-facing {@link AuditLogEntryResponse}, keeping JaVers' own types
 * out of any controller.
 */
@Service
public class DefaultAuditQueryService implements AuditQueryService {

    private final Javers javers;

    public DefaultAuditQueryService(Javers javers) {
        this.javers = javers;
    }

    @Override
    public List<AuditLogEntryResponse> findHistory(Class<?> entityType, Object entityId) {
        JqlQuery query = QueryBuilder.byInstanceId(entityId, entityType).build();
        List<CdoSnapshot> snapshots = javers.findSnapshots(query);

        // JaVers doesn't guarantee findSnapshots' return order, so sort explicitly -- oldest
        // first, so each entry's "before" is simply the state extracted from the one immediately
        // preceding it in this same pass, without a second query per row.
        List<CdoSnapshot> chronological =
                snapshots.stream().sorted(Comparator.comparing(s -> s.getCommitMetadata().getCommitDate())).toList();

        List<AuditLogEntryResponse> result = new ArrayList<>(chronological.size());
        Map<String, Object> previousState = null;
        for (CdoSnapshot snapshot : chronological) {
            result.add(toResponse(snapshot, previousState));
            previousState = extractState(snapshot);
        }
        Collections.reverse(result); // newest first, matching findRecent/findByAction
        return result;
    }

    @Override
    public List<AuditLogEntryResponse> findRecent(int limit) {
        JqlQuery query = QueryBuilder.anyDomainObject().limit(limit).build();
        return javers.findSnapshots(query).stream().map(s -> toResponse(s, null)).toList();
    }

    @Override
    public List<AuditLogEntryResponse> findByAction(String action, int limit) {
        JqlQuery query = QueryBuilder.anyDomainObject()
                .withCommitProperty(DefaultAuditActionRecorder.ACTION_PROPERTY, action)
                .limit(limit)
                .build();
        return javers.findSnapshots(query).stream().map(s -> toResponse(s, null)).toList();
    }

    /** @param before the previous commit's full state for this SAME entity, or {@code null} when
     *     that isn't known/computed (see {@link #findRecent}/{@link #findByAction}) -- spanning
     *     many different entities per call, so computing it per row would mean one extra JaVers
     *     query per result rather than one query total, unlike {@link #findHistory}, which
     *     already fetches every commit for a single entity and can just pair up neighbors. */
    private static AuditLogEntryResponse toResponse(CdoSnapshot snapshot, Map<String, Object> before) {
        return new AuditLogEntryResponse(
                snapshot.getGlobalId().value(),
                snapshot.getType().name(),
                snapshot.getCommitMetadata().getProperties().get(DefaultAuditActionRecorder.ACTION_PROPERTY),
                snapshot.getCommitMetadata().getAuthor(),
                snapshot.getCommitMetadata().getCommitDate(),
                snapshot.getCommitMetadata().getId().toString(),
                before,
                extractState(snapshot));
    }

    private static Map<String, Object> extractState(CdoSnapshot snapshot) {
        CdoSnapshotState snapshotState = snapshot.getState();
        Map<String, Object> state = new LinkedHashMap<>();
        for (String property : snapshotState.getPropertyNames()) {
            state.put(property, snapshotState.getPropertyValue(property));
        }
        return state;
    }
}
