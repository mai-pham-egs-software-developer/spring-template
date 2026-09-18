package com.my.craft.security.service.outbox;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.my.craft.security.domain.OutboxEvent;

/**
 * Delivers {@link OutboxEvent} rows written by {@code DefaultUserAdminService.create} -- the
 * async half of the dual write: the DB write is synchronous and transactional, calling the
 * identity provider is {@link OutboxTransactionalOperations#processNextEvent}'s job, retried with
 * backoff on a transient failure and parked in the dead-letter queue ({@code OutboxStatus.DEAD})
 * or flagged {@code UserStatus.CONFLICT}/{@code FAILED} otherwise. See
 * {@code backend/docs/user-outbox.md}.
 */
@Component
public class OutboxWorker {

    private final OutboxTransactionalOperations operations;

    public OutboxWorker(OutboxTransactionalOperations operations) {
        this.operations = operations;
    }

    /** One tick drains every ready event back-to-back (instead of waiting a full {@code
     * pollInterval} per event), so a burst of creates doesn't trickle out one every few seconds.
     * Calls {@link OutboxTransactionalOperations} (a separate bean, not a method on this class)
     * so its {@code @Transactional} actually takes effect -- see that class's javadoc. */
    @Scheduled(fixedDelayString = "${app.outbox.poll-interval:PT2S}")
    public void drain() {
        while (operations.processNextEvent()) {
            // keep going until lockNextReady finds nothing left
        }
    }
}
