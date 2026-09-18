package com.my.craft.security.domain;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * One row per domain event awaiting async delivery -- table {@code outbox}. Written in the SAME
 * local transaction as the change it describes (see {@code DefaultUserAdminService.create}), so
 * the two can never diverge: either both commit, or neither does. {@code
 * com.my.craft.security.service.outbox.OutboxWorker} polls {@code status = PENDING} rows with
 * {@code SELECT ... FOR UPDATE SKIP LOCKED} ({@link
 * com.my.craft.security.repository.OutboxEventJpaRepository#lockNextReady}) and delivers them --
 * {@link #scheduleRetry} backs off on a transient failure, {@link #markDead} parks a row that's
 * exhausted its attempts (or hit a permanent failure) in the dead-letter queue. See
 * {@code backend/docs/user-outbox.md}.
 */
@Entity
@Table(name = "outbox")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OutboxEvent {

    /** The only event type this template emits today -- {@code aggregateId} names the {@link
     * User} row it's about, {@code payload} carries the fields {@code
     * com.my.craft.security.service.outbox.OutboxWorker} needs to (re)try creating it upstream. */
    public static final String USER_CREATE_REQUESTED = "USER_CREATE_REQUESTED";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, updatable = false)
    private String aggregateId;

    @Column(nullable = false, updatable = false)
    private String eventType;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, updatable = false, columnDefinition = "jsonb")
    private Map<String, Object> payload;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OutboxStatus status;

    @Column(nullable = false)
    private int attempts;

    @Column(nullable = false)
    private Instant nextRetryAt;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    private Instant processedAt;

    public OutboxEvent(String aggregateId, String eventType, Map<String, Object> payload) {
        this.aggregateId = aggregateId;
        this.eventType = eventType;
        this.payload = payload;
        this.status = OutboxStatus.PENDING;
        this.attempts = 0;
        this.nextRetryAt = Instant.now();
    }

    public void markProcessed() {
        this.status = OutboxStatus.PROCESSED;
        this.processedAt = Instant.now();
    }

    /** Bumps {@link #attempts} and pushes {@link #nextRetryAt} out by {@code backoff}; the caller
     * (see {@code OutboxWorker.scheduleRetry}) is responsible for calling {@link #markDead}
     * instead once {@link #attempts} exceeds the configured max. */
    public void scheduleRetry(Duration backoff) {
        this.attempts++;
        this.nextRetryAt = Instant.now().plus(backoff);
    }

    public void markDead() {
        this.status = OutboxStatus.DEAD;
    }
}
