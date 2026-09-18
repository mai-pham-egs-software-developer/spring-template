package com.my.craft.security.repository;

import java.time.Instant;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.my.craft.security.domain.OutboxEvent;
import com.my.craft.security.domain.OutboxStatus;

public interface OutboxEventJpaRepository extends JpaRepository<OutboxEvent, Long> {

    /**
     * Claims (locks) the single oldest ready {@code PENDING} row, skipping any already locked by
     * another worker instance -- {@code SKIP LOCKED} is what lets several {@code OutboxWorker}s
     * run concurrently without ever processing the same event twice. Must be called inside the
     * caller's own transaction: the row lock is held until that transaction commits or rolls
     * back, which is what actually enforces the mutual exclusion -- see {@code
     * OutboxTransactionalOperations.processNextEvent}.
     */
    @Query(
            value = "SELECT * FROM outbox WHERE status = 'PENDING' AND next_retry_at <= :now ORDER BY id LIMIT 1 FOR UPDATE SKIP LOCKED",
            nativeQuery = true)
    Optional<OutboxEvent> lockNextReady(@Param("now") Instant now);

    boolean existsByAggregateIdAndStatus(String aggregateId, OutboxStatus status);

    long countByStatus(OutboxStatus status);
}
