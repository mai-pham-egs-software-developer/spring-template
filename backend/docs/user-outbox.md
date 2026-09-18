# User/Keycloak dual write — the outbox pattern

Creating a `User` has to write to two systems that don't share a transaction: this app's own
`users` table, and Keycloak (via its admin REST API). `POST /operators/users` no longer calls
Keycloak itself — it writes the local row plus an outbox event, in one transaction, and returns
immediately. A background worker delivers the event asynchronously, with retries, backoff, a
dead-letter queue, and a periodic reconciliation pass as the safety net. This gives up synchronous
"the account exists in Keycloak by the time this request returns" in exchange for never losing a
create (an app crash mid-request, or Keycloak being briefly down, can't corrupt or drop it) and
never silently diverging between the two systems.

## Schema

```
users (com.my.craft.security.domain.User)
    id              PK, app-generated (UUID), NOT the Keycloak subject
    username        NOT NULL, unique
    email           NOT NULL, unique
    name            NOT NULL   -- this app's own display name
    keycloak_id     NULL       -- filled in once OutboxWorker syncs successfully
    status          NOT NULL   -- PENDING | ACTIVE | CONFLICT | FAILED  (UserStatus)
    created_at, updated_at

outbox (com.my.craft.security.domain.OutboxEvent)
    id              PK
    aggregate_id    NOT NULL   -- = users.id
    event_type      NOT NULL   -- "USER_CREATE_REQUESTED" (OutboxEvent.USER_CREATE_REQUESTED)
    payload         jsonb NOT NULL  -- {username, email, name}
    status          NOT NULL   -- PENDING | PROCESSED | DEAD  (OutboxStatus)
    attempts        int, default 0
    next_retry_at   NOT NULL
    created_at, processed_at
```

`id` is deliberately **not** the Keycloak subject claim anymore (unlike the rest of this template's
original design) — the row has to exist, with a stable id other tables can point at (see
`UserOrganizationRole`), before Keycloak has ever been called. This has no effect on RBAC
enforcement: `CasbinAuthorizationManager` is keyed by the Keycloak subject straight off the JWT,
never by `User.id` (see `docs/security.md`). It only means a hand-written, fine-grained policy row
targeting a specific `/operators/users/{id}` resource now names the local id, not the Keycloak
subject.

## State machines

```
User.status                                  OutboxEvent.status
                                              
  PENDING ──sync OK────────▶ ACTIVE            PENDING ──delivered──────▶ PROCESSED
     │                                            │
     ├──409, not same person──▶ CONFLICT          ├──transient failure──▶ (stays PENDING,
     │                                            │                       next_retry_at pushed out)
     └──permanent 4xx──────────▶ FAILED           └──max attempts / permanent failure──▶ DEAD
```

## Flow

**A — `POST /operators/users` (`DefaultUserAdminService.create`, synchronous)**

One local transaction: insert the `User` row (`status = PENDING`), insert the matching
`OutboxEvent`, commit, return `202 Accepted` with the row (still `PENDING`). A unique-constraint
violation on `username`/`email` rolls the whole thing back and surfaces as `409 Conflict`
(`UserAlreadyExistsException`) — this is what makes two concurrent creates of the same username
race safely (see TH1 below): only one can ever commit.

**B/C — `OutboxWorker.drain` (scheduled, `app.outbox.poll-interval`) →
`OutboxTransactionalOperations.processNextEvent`**

Each tick claims and handles ready `PENDING` outbox rows one at a time, draining all of them
before going back to sleep. Claiming uses
`OutboxEventJpaRepository.lockNextReady` — `SELECT ... FOR UPDATE SKIP LOCKED` — inside the same
transaction as the rest of the event's handling, so several worker instances can run concurrently
without ever double-processing one event; the row lock (and, deliberately, an open transaction
around the Keycloak HTTP call) is what enforces that.

The transactional work is deliberately split into its own bean
(`OutboxTransactionalOperations`), not a method on `OutboxWorker`/`UserReconciliationJob`
themselves: a `@Scheduled` method calling a `@Transactional` method declared on that *same* class
is Spring's self-invocation pitfall — the call bypasses the AOP proxy, so `@Transactional`
silently does nothing. That's exactly what caused an early version of this to loop forever
reprocessing the same event at network-round-trip speed (the row lock releasing immediately, the
entities it mutated staying detached with nothing to flush them, so the DB row never actually left
`PENDING`) — see the commit that split this out if you're tempted to inline it back.

`UserService.tryCreateUser(username, email)` (`KeycloakUserService`) never throws for an ordinary
failure — every outcome is a value (`CreateUserOutcome`, a sealed interface):

| Outcome | Keycloak response | `OutboxWorker` does |
|---|---|---|
| `Created(keycloakId)` | `201` | `User.activate(keycloakId)`, event → `PROCESSED` |
| `Adopted(keycloakId)` | `409` + same person (email matches) | same as `Created`, existing credentials left untouched |
| `Conflict` | `409` + not confidently the same person, or the follow-up lookup can't even find it | `User.markConflict()`, event → `PROCESSED` (a human takes it from here) |
| `TransientFailure` | network error, timeout, `5xx` | retry with backoff (see below) |
| `PermanentFailure` | any other `4xx` | `User.markFailed()`, event → `DEAD` |

A brand-new (`Created`) account gets a random 16-character temporary password
(`KeycloakUserService.generateTemporaryPassword`), set as `temporary=true` so Keycloak forces a
change at first login. It's logged (`log.info`) and not persisted anywhere or returned through any
API — this template has no SMTP/invite-email configured, so actually delivering it to the new user
is intentionally left as a follow-up decision (see "Not implemented" below), not something this
change invents a bespoke secret-delivery mechanism for. An **adopted** account's password is never
touched — it already belongs to someone/something else.

**Retry backoff** — `OutboxTransactionalOperations.scheduleRetry`: exponential, `2^attempts`
seconds (2, 4, 8, 16, ...), capped at `app.outbox.max-attempts` (default 6) before the event moves
to `DEAD`.

## Reconciliation (`UserReconciliationJob`, `app.outbox.reconciliation-cron`, default every 15m)

The safety net behind the event-driven path above, one `PENDING` user at a time (its own
transaction each, so one failure doesn't roll back progress on the rest of the run):

1. **Heal an already-synced account** — look the username up in Keycloak directly
   (`UserService.findKeycloakIdByUsername`); if it's there, `activate()` the local row instead of
   trying again. Covers a worker that crashed after the Keycloak call succeeded but before it
   recorded that.
2. **Re-queue a stuck user** — a `PENDING` row older than `app.outbox.stale-pending-threshold`
   with no `PENDING` outbox event for it (`OutboxEventJpaRepository.existsByAggregateIdAndStatus`)
   gets a fresh `OutboxEvent`. Covers a lost event (there's no other way to end up `PENDING` with
   nothing in flight).
3. **Alert** — logs a warning (`log.warn`) if any `User` is `CONFLICT`/`FAILED` or any
   `OutboxEvent` is `DEAD`. This template has no paging/notification integration, so "alert" here
   means "visible in the logs," not a push notification — wire it up to whatever this deployment
   already uses for that.

## The two race conditions

**TH1 — two concurrent creates of the same username, before either's event is processed.** Purely
local to this app; Keycloak hasn't been called yet so it can't be the tiebreaker. The unique
constraint on `users.username`/`users.email` is: both inserts happen in flow A above, so the
second one's `DataIntegrityViolationException` rolls its whole transaction back (row *and* outbox
event) and surfaces as `409`.

**TH2 — Keycloak already has a matching account from somewhere else** (its own admin console,
another app, an LDAP sync, ...) by the time `OutboxWorker` gets to the event. This app's local
insert already succeeded (`PENDING`); the Keycloak call then hits `409`.
`KeycloakUserService.resolveConflict` decides: **adopt** (username + email both match — treat as
the same person, link `keycloakId`, leave credentials alone) or **flag `CONFLICT`** (email
mismatch, or the follow-up lookup finds nothing at all — drift). Never auto-delete the Keycloak
account to "start clean," and never retry a `409` as if it might resolve itself — both are handled
explicitly as terminal outcomes for that attempt, not looped on.

## Not implemented (scoped out, follow-ups if needed)

- **Orphan detection** (a Keycloak account with no matching local `User` at all) — the original
  design's step (29). Doing this properly means paginating Keycloak's whole user list and diffing
  against `users`, and the right response is genuinely deployment-specific (delete / adopt into a
  new local row / log-only) — left out rather than guessed at.
- **Delivering the generated temporary password** to the new user — see above; this template has
  no email/SMS integration to plug it into.
- **Update/deactivate parity** — `UserAdminService.update`/`delete` still only touch the local
  row; a deleted `User` here doesn't deactivate or remove the Keycloak account. `UserService`'s
  `updateUser`/`deactivateUser` methods are still stubs, same as before this change.
