package com.my.craft.security.domain;

/** {@code OutboxEvent.status}. */
public enum OutboxStatus {

    /** Not yet delivered -- eligible for {@code OutboxWorker} to claim once {@code nextRetryAt}
     * has passed. */
    PENDING,

    /** Delivered successfully (or turned out to be moot, e.g. its aggregate was already resolved
     * by an earlier duplicate event). */
    PROCESSED,

    /** Exhausted its retry budget, or hit a non-retryable failure -- parked in the dead-letter
     * queue for manual triage instead of retrying forever. */
    DEAD
}
