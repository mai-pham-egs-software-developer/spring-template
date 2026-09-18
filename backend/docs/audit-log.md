# `modules/audit-log` — full before/after change history via JaVers

Records the complete state of an entity at every commit (create/update/delete), not just a diff —
"what did this row look like right after each change." Built on
[JaVers](https://javers.org/)'s Spring Boot SQL integration, which persists its own history tables
(`jv_*`) via the app's existing `DataSource`, alongside (not instead of) this app's own JPA tables.

> **Version pin, verified** — `pom.xml` pins `javers-spring-boot-starter-sql:7.8.0`, confirmed
> against Maven Central's search API to be the highest version actually published (not e.g.
> `7.6.5`/`7.11.8`, both of which were tried in earlier drafts of this file and don't exist). Its
> own POM targets Spring Boot 3.3.4 (Jakarta EE), compatible with this reactor's 3.5.16 parent.
> Every JaVers method `DefaultAuditQueryService` calls was checked against this exact version's
> source on GitHub (`javers/javers`, tag `javers-7.8.0`) and matches. If a future bump changes
> this pin, re-check `DefaultAuditQueryService` (`CdoSnapshot.getGlobalId().value()`, `.getType()`,
> `.getCommitMetadata()`, `CdoSnapshotState.getPropertyNames()`/`getPropertyValue`) against that
> new version's actual API — this project still couldn't run `mvn compile` itself to confirm.

## How to mark an action for audit

**No blanket per-repository auto-audit.** An earlier version of this module used
`@JaversSpringDataAuditable` on the JPA repository, which commits a snapshot on *every*
`save()`/`delete()` regardless of why it was called. That's been replaced with explicit,
opt-in marking: only a business action a caller actually records shows up in the audit log.
`UserJpaRepository`/`OrganizationJpaRepository`/`RoleJpaRepository` no longer carry the
annotation.

The primitive is `AuditActionRecorder` (`com.my.craft.auditlog.service`), called at the exact
point in a service method where the marked action happens:

```java
public interface AuditActionRecorder {
    void record(String action, Object entity);          // create/update -- normal snapshot
    void recordDeletion(String action, Object entity);   // delete -- TERMINAL snapshot
}
```

`entity` must be the real JPA-mapped domain object (not a DTO) so JaVers can diff its actual
property state — call this right after the entity's own `repository.save(...)` (for `record`) so
JaVers sees the just-persisted state, and while still inside the surrounding `@Transactional`
boundary if the entity has any lazy-loaded relationships JaVers needs to read. Example, from
`DefaultUserAdminService.create`:

```java
User saved = userRepository.save(user);
auditActionRecorder.record("USER_CREATED", saved);
```

`action` is a plain string naming the business action (e.g. `"USER_CREATED"`, `"ROLE_UPDATED"`,
`"ORGANIZATION_DELETED"`) — it's stored as a JaVers *commit property* (`Javers.commit(author,
entity, Map.of("action", action))`), not a special JaVers concept, which is what makes
`AuditQueryService.findByAction`/`GET /operators/audit-log?action=...` possible (`QueryBuilder
.withCommitProperty("action", action)`). Currently marked, in `security`:

| Action | Where |
|---|---|
| `USER_CREATED` / `USER_UPDATED` / `USER_DELETED` | `DefaultUserAdminService` |
| `ORGANIZATION_CREATED` / `ORGANIZATION_UPDATED` / `ORGANIZATION_DELETED` | `DefaultOrganizationService` |
| `ROLE_CREATED` / `ROLE_UPDATED` / `ROLE_DELETED` | `DefaultRoleService` |
| `ORGANIZATION_MEMBER_ADDED` / `ORGANIZATION_MEMBER_UPDATED` / `ORGANIZATION_MEMBER_REMOVED` | `DefaultOrganizationMembershipService` |

`addMember`/`updateMember` both delete any existing `UserOrganizationRole` row for that
`(user, organization)` pair and insert a fresh one (`DefaultOrganizationMembershipService`'s own
"at most one role per pair" contract, see its class javadoc) rather than mutating a row in place --
so the entity JaVers snapshots there always has a brand-new surrogate id, and its own
`snapshotType` is always `INITIAL`, never `UPDATE`, even for `updateMember`. That's a property of
the underlying delete+recreate pattern, not a bug here: `markedAction` (`ORGANIZATION_MEMBER_UPDATED`)
still correctly identifies what happened via `findByAction`/`findRecent`; only a `findHistory` on
that one `UserOrganizationRole` id specifically would look like it has no prior version, because it
genuinely doesn't -- the "history" of a membership is really the sequence of `ADDED`/`UPDATED`/
`REMOVED` marked actions for that `(user, organization)` pair, not one entity's own snapshotType
timeline.

Not yet marked (a deliberate scope cut, not an oversight): the outbox-driven user-activation flow
(`OutboxTransactionalOperations`) — mark that the same way (inject `AuditActionRecorder`, call
`record`/`recordDeletion` after the mutating repository call) if it should show up in the audit
log too.

**Author attribution**: `javers-spring-boot-starter-sql` auto-configures a
`SpringSecurityAuthorProvider` when Spring Security is on the classpath and no other
`AuthorProvider` bean exists — it reads the current `Authentication.getName()` at commit time.
`DefaultAuditActionRecorder` gets this same bean injected (`org.javers.spring.auditable.AuthorProvider`)
and calls `.provide()` itself, since it commits explicitly rather than through JaVers' own
repository-wrapping aspect. No extra wiring needed in this app, since `security` already puts a
`CustomAuthenticationToken` in the security context on every authenticated request.

## Querying the history

`AuditQueryService` (`com.my.craft.auditlog.service`) is the only thing a Java caller needs — it
hides every JaVers type behind `AuditLogEntryResponse`:

```java
List<AuditLogEntryResponse> findHistory(Class<?> entityType, Object entityId);  // one instance
List<AuditLogEntryResponse> findRecent(int limit);                              // across everything, regardless of marked action
List<AuditLogEntryResponse> findByAction(String action, int limit);             // scoped to one marked action, e.g. "USER_CREATED"
```

`entityType` is a plain `Class<?>` the caller supplies (e.g. `User.class`) — this module has no
dependency on `modules/security` or any other `modules/*` library, so it can't and shouldn't name
any specific entity itself.

`AuditLogEntryResponse.snapshotType` (`INITIAL`/`UPDATE`/`TERMINAL`) is JaVers' own low-level
classification of the commit; `markedAction` is the business action name passed to
`AuditActionRecorder` (e.g. `"USER_CREATED"`) — the two are independent axes, don't conflate them
(a field named plain `action` in an earlier version of this DTO held what's now `snapshotType`,
renamed specifically to avoid that confusion once `markedAction` was introduced).

**Before/after**: `after` is always populated (the entity's full property state as of that
commit); `before` (the state immediately prior, for the same entity) is populated **only by
`findHistory`** — fetching one entity's whole timeline is a single JaVers query, so pairing each
commit with its predecessor there is nearly free (`DefaultAuditQueryService` sorts the fetched
snapshots chronologically and just carries the previous iteration's state forward). `findRecent`/
`findByAction` span many different entities per call; giving every row a real `before` there would
mean a second JaVers query per result rather than one query total, so they deliberately leave
`before = null` instead.

**`/audit-log` page**: `AuditLogPage.jsx` lists via `listRecentAuditLog` (`before` always `null`,
per above), so expanding a row can't show a diff from that data alone. `AuditEntryDetail` closes
that gap on demand, per row, only when actually expanded (not for the whole visible list up
front): it calls `listEntityHistory` (`GET /operators/audit-log/history`, exported from
`api/auditLog.js` for this) for THAT ONE entity (its class name and id parsed straight out of the
row's own `globalId`), finds the entry with the matching `commitId` in the returned history (which
already has `before` computed server-side), and hands that + the row's own `after` to
`AuditStateTable`, which renders the actual before/after diff (changed properties struck
through/bolded) instead of the plain property list it falls back to when `before` is still
unknown (`INITIAL` snapshots legitimately have no `before` either, same fallback).

## The REST API — `web.AuditLogController`, self-hosted in THIS module

Deliberately not in `modules/security` (where every other `/operators/**` controller lives):
`security` already depends on `audit-log` for `AuditActionRecorder`, so a controller here that
needed `security`'s `User`/`Organization`/`Role` classes or its `@RequiresPermission` annotation
would need a dependency back the other way — a cycle Maven flatly refuses to build. Consequences
of keeping it one-way:

- **`GET /operators/audit-log?limit=100`** — `findRecent`, entity-agnostic. Add `&action=<name>`
  to call `findByAction` instead and scope the result to one marked action.
- **`GET /operators/audit-log/history?entityType=<fully-qualified class name>&entityId=<id>`** —
  `findHistory`, generic instead of one endpoint per entity (no `/users/{id}`-style routes here
  anymore). `entityType` is resolved by reflection (`Class.forName`) rather than a compile-time
  reference. This is safe despite looking like it accepts an arbitrary class name: JaVers only
  ever returns data for a class some `AuditActionRecorder.record`/`recordDeletion` call has
  actually committed a snapshot for, so naming an unmarked or nonexistent class just yields
  `400`/an empty list, nothing more. The frontend (`api/auditLog.js`) hardcodes the FQCNs it needs
  (e.g. `com.my.craft.security.domain.User`) as plain strings for this reason.
- **No `@RequiresPermission`** — that annotation lives in `security`, equally unreachable without
  the same cycle. `CasbinAuthorizationManager` still default-denies every `/operators/**` path
  without it, falling back to `objectType = request path, action = HTTP method` (see its javadoc
  and `docs/security.md`) — the same fallback `CasbinPolicyController`/`MeController` already rely
  on, not a new gap. The master-org bypass added there also still applies, since it only checks
  the path prefix.
- `entityId` is always a `String` here (an HTTP query param can't be anything else), even for
  `Organization`/`Role` whose real `@Id` is `Long`. This still matches correctly: JaVers'
  `GlobalId` equality is defined over its canonical `value()`/`toString()` form, not the id's
  runtime type, so `"5"` and `Long 5L` produce the same comparison key.

## What's NOT wired up

- Only the actions listed in the table above are currently marked. `Permission`/
  `UserOrganizationRole`-related changes (and anything in `file-storage`) aren't — inject
  `AuditActionRecorder` into the relevant service and call `record`/`recordDeletion` the same way
  if they should be audited too.
- No retention/cleanup policy on JaVers' own `jv_*` tables — they grow forever by default.
