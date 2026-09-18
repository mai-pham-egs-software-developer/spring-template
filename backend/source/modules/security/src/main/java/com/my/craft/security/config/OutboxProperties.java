package com.my.craft.security.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Getter;
import lombok.Setter;

/** Binds {@code app.outbox.*} -- tuning for {@code com.my.craft.security.service.outbox}'s {@code
 * OutboxWorker}/{@code UserReconciliationJob}. See {@code backend/docs/user-outbox.md}. */
@Getter
@Setter
@ConfigurationProperties(prefix = "app.outbox")
public class OutboxProperties {

    /** How long {@code OutboxWorker} sleeps after finding no ready {@code PENDING} event before
     * polling again -- it drains every ready event back-to-back within one tick first, so this
     * only bounds the worst-case delay for a newly-created event, not steady-state throughput. */
    private Duration pollInterval = Duration.ofSeconds(2);

    /** A retry beyond this many attempts moves the event to {@code DEAD} instead of being
     * scheduled again. */
    private int maxAttempts = 6;

    /** How long a {@code User} can sit {@code PENDING} before {@code UserReconciliationJob}
     * re-queues it (its outbox event was presumably lost) -- a safety net, not the normal path. */
    private Duration stalePendingThreshold = Duration.ofMinutes(15);
}
