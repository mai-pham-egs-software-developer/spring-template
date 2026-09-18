package com.my.craft.security.service.outbox;

import java.time.Instant;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.my.craft.security.config.OutboxProperties;
import com.my.craft.security.domain.OutboxStatus;
import com.my.craft.security.domain.User;
import com.my.craft.security.domain.UserStatus;
import com.my.craft.security.repository.OutboxEventJpaRepository;
import com.my.craft.security.repository.UserJpaRepository;

/**
 * The safety net behind {@link OutboxWorker}'s normal event-driven path -- runs periodically and
 * fixes drift the event flow alone wouldn't catch: a {@code PENDING} user whose outbox event was
 * somehow lost, or one where the identity provider actually already has the account (e.g.
 * created directly through a different path) but the local row was never linked -- see {@link
 * OutboxTransactionalOperations#reconcileUser}. Orphan accounts on the provider side (present
 * there, matching nothing locally) are intentionally NOT handled here -- see
 * {@code backend/docs/user-outbox.md}'s note on why auto-deleting or auto-adopting one without a
 * human isn't safe.
 */
@Component
public class UserReconciliationJob {

    private static final Logger log = LoggerFactory.getLogger(UserReconciliationJob.class);

    private final UserJpaRepository userRepository;
    private final OutboxEventJpaRepository outboxRepository;
    private final OutboxTransactionalOperations operations;
    private final OutboxProperties properties;

    public UserReconciliationJob(
            UserJpaRepository userRepository,
            OutboxEventJpaRepository outboxRepository,
            OutboxTransactionalOperations operations,
            OutboxProperties properties) {
        this.userRepository = userRepository;
        this.outboxRepository = outboxRepository;
        this.operations = operations;
        this.properties = properties;
    }

    @Scheduled(cron = "${app.outbox.reconciliation-cron:0 */15 * * * *}")
    public void reconcile() {
        List<User> pending = userRepository.findByStatus(UserStatus.PENDING);
        Instant staleBefore = Instant.now().minus(properties.getStalePendingThreshold());
        // operations.reconcileUser is a separate bean's @Transactional method (not one declared
        // on this class) -- see OutboxTransactionalOperations' javadoc for why that split matters.
        for (User user : pending) {
            operations.reconcileUser(user.getId(), staleBefore);
        }
        alertOnAttentionNeeded();
    }

    private void alertOnAttentionNeeded() {
        long conflicts = userRepository.countByStatus(UserStatus.CONFLICT);
        long failed = userRepository.countByStatus(UserStatus.FAILED);
        long dead = outboxRepository.countByStatus(OutboxStatus.DEAD);
        if (conflicts > 0 || failed > 0 || dead > 0) {
            log.warn(
                    "reconciliation: {} user(s) CONFLICT, {} FAILED, {} outbox event(s) DEAD -- needs admin attention",
                    conflicts,
                    failed,
                    dead);
        }
    }
}
