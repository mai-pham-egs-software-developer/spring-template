package com.my.craft.auditlog.service;

import java.util.Map;

import org.javers.core.Javers;
import org.javers.spring.auditable.AuthorProvider;
import org.springframework.stereotype.Service;

@Service
public class DefaultAuditActionRecorder implements AuditActionRecorder {

    static final String ACTION_PROPERTY = "action";

    private final Javers javers;
    private final AuthorProvider authorProvider;

    public DefaultAuditActionRecorder(Javers javers, AuthorProvider authorProvider) {
        this.javers = javers;
        this.authorProvider = authorProvider;
    }

    @Override
    public void record(String action, Object entity) {
        javers.commit(authorProvider.provide(), entity, Map.of(ACTION_PROPERTY, action));
    }

    @Override
    public void recordDeletion(String action, Object entity) {
        javers.commitShallowDelete(authorProvider.provide(), entity, Map.of(ACTION_PROPERTY, action));
    }
}
