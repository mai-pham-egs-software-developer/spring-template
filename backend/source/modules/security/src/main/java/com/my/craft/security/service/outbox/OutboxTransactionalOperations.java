package com.my.craft.security.service.outbox;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.my.craft.security.config.OutboxProperties;
import com.my.craft.security.domain.OutboxEvent;
import com.my.craft.security.domain.OutboxStatus;
import com.my.craft.security.domain.User;
import com.my.craft.security.domain.UserStatus;
import com.my.craft.security.repository.OutboxEventJpaRepository;
import com.my.craft.security.repository.UserJpaRepository;
import com.my.craft.security.service.CreateUserOutcome;
import com.my.craft.security.service.UserService;

/**
 * The actual {@code @Transactional} units of work behind {@link OutboxWorker} and {@link
 * UserReconciliationJob}. Deliberately its own bean, not a method on either of those: a {@code
 * @Scheduled} method calling a {@code @Transactional} method declared on that SAME class is
 * Spring's classic self-invocation pitfall -- the call bypasses the AOP proxy entirely, so {@code
 * @Transactional} silently does nothing. That bug is exactly what previously let {@code
 * OutboxWorker.drain} claim the same row's lock and release it immediately (each repository call
 * auto-committing on its own), mutate now-detached entities with nothing to flush them, and loop
 * forever reprocessing the same never-actually-updated {@code OutboxEvent} -- see
 * {@code backend/docs/user-outbox.md}. Calling through THIS bean's own injected reference (a real
 * cross-bean call) goes through the proxy correctly.
 */
@Component
class OutboxTransactionalOperations {

    private static final Logger log = LoggerFactory.getLogger(OutboxTransactionalOperations.class);

    private final OutboxEventJpaRepository outboxRepository;
    private final UserJpaRepository userRepository;
    private final UserService userService;
    private final OutboxProperties properties;

    OutboxTransactionalOperations(
            OutboxEventJpaRepository outboxRepository,
            UserJpaRepository userRepository,
            UserService userService,
            OutboxProperties properties) {
        this.outboxRepository = outboxRepository;
        this.userRepository = userRepository;
        this.userService = userService;
        this.properties = properties;
    }

    /**
     * Claims and handles exactly one event -- the {@code FOR UPDATE SKIP LOCKED} lock {@link
     * OutboxEventJpaRepository#lockNextReady} takes is held for the whole method, including the
     * Keycloak HTTP call, which is what stops two worker instances from ever processing the same
     * event concurrently.
     *
     * @return whether an event was actually found (the caller uses this to decide whether to loop
     *     immediately or wait for the next scheduled tick).
     */
    @Transactional
    boolean processNextEvent() {
        Optional<OutboxEvent> claimed = outboxRepository.lockNextReady(Instant.now());
        if (claimed.isEmpty()) {
            return false;
        }
        handle(claimed.get());
        return true;
    }

    private void handle(OutboxEvent event) {
        User user = userRepository.findById(event.getAggregateId()).orElse(null);
        if (user == null) {
            log.warn("outbox event {} references user {} which no longer exists -- dropping", event.getId(), event.getAggregateId());
            markProcessed(event);
            return;
        }
        if (user.getStatus() != UserStatus.PENDING) {
            // Already resolved -- a previous attempt succeeded and this is a stale duplicate
            // event, or an admin already moved the user to CONFLICT/FAILED by hand.
            markProcessed(event);
            return;
        }

        CreateUserOutcome outcome = userService.tryCreateUser(user.getUsername(), user.getEmail());
        switch (outcome) {
            case CreateUserOutcome.Created created -> activate(user, event, created.keycloakId());
            case CreateUserOutcome.Adopted adopted -> activate(user, event, adopted.keycloakId());
            case CreateUserOutcome.Conflict ignored -> {
                user.markConflict();
                userRepository.save(user);
                markProcessed(event);
            }
            case CreateUserOutcome.TransientFailure transientFailure -> scheduleRetry(event, transientFailure.message());
            case CreateUserOutcome.PermanentFailure permanentFailure -> {
                user.markFailed();
                userRepository.save(user);
                event.markDead();
                outboxRepository.save(event);
                log.error("outbox event {} permanently failed for user {}: {}", event.getId(), user.getId(), permanentFailure.message());
            }
        }
    }

    private void activate(User user, OutboxEvent event, String keycloakId) {
        user.activate(keycloakId);
        userRepository.save(user);
        markProcessed(event);
    }

    private void markProcessed(OutboxEvent event) {
        event.markProcessed();
        outboxRepository.save(event);
    }

    private void scheduleRetry(OutboxEvent event, String reason) {
        if (event.getAttempts() + 1 > properties.getMaxAttempts()) {
            event.markDead();
            outboxRepository.save(event);
            log.error("outbox event {} exceeded {} attempts, moved to DEAD: {}", event.getId(), properties.getMaxAttempts(), reason);
            return;
        }
        Duration backoff = Duration.ofSeconds(1L << (event.getAttempts() + 1)); // 2, 4, 8, 16, ...
        event.scheduleRetry(backoff);
        outboxRepository.save(event);
        log.info("outbox event {} transient failure, retry #{} in {}: {}", event.getId(), event.getAttempts(), backoff, reason);
    }

    /** One user per transaction -- so a failure reconciling one doesn't roll back progress
     * already made on the others in the same {@link UserReconciliationJob#reconcile} run, and
     * re-loads the user fresh rather than carrying a possibly-stale instance across that whole
     * (potentially slow, Keycloak-calling) loop. */
    @Transactional
    void reconcileUser(String userId, Instant staleBefore) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null || user.getStatus() != UserStatus.PENDING) {
            return; // resolved (or deleted) since UserReconciliationJob.reconcile listed it
        }

        // The account might already exist upstream -- created directly there, or a worker
        // crashed after the call succeeded but before it recorded that. Heal the link instead of
        // creating a duplicate outbox event.
        Optional<String> existingKeycloakId = userService.findKeycloakIdByUsername(user.getUsername());
        if (existingKeycloakId.isPresent()) {
            log.info("reconciliation: linking user {} to already-existing account {}", user.getId(), existingKeycloakId.get());
            user.activate(existingKeycloakId.get());
            userRepository.save(user);
            return;
        }

        if (user.getCreatedAt().isAfter(staleBefore)) {
            return; // still within its normal retry window -- leave it to OutboxWorker
        }
        if (outboxRepository.existsByAggregateIdAndStatus(user.getId(), OutboxStatus.PENDING)) {
            return; // an event is still in flight (retry backoff, or mid-processing)
        }

        log.warn("reconciliation: user {} PENDING with no in-flight outbox event -- re-queuing", user.getId());
        outboxRepository.save(new OutboxEvent(
                user.getId(),
                OutboxEvent.USER_CREATE_REQUESTED,
                Map.of("username", user.getUsername(), "email", user.getEmail(), "name", user.getName())));
    }
}
