# `modules/security` — dynamic per-path auth config

`SecurityConfig` (`com.my.craft.security.config`) does not hard-code which endpoints need which
auth mechanism. Instead it reads a list of rules straight off the top-level `security:` key in
`application.yml` and builds the Spring Security filter chains from it at startup.

## Config schema

```yaml
security:
  - path: /api/admin/*
    auth-type: BASIC
    username: admin
    password: admin
  - path: /monitor/*
    auth-type: BEARER
```

Each entry (`SecurityRuleProperties`):

| Field | Required | Meaning |
|---|---|---|
| `path` | yes | Ant-style pattern the rule applies to (passed straight to `HttpSecurity#securityMatcher`). |
| `auth-type` | yes | One of `AuthType`: `BASIC` or `BEARER`. |
| `username` / `password` | `BASIC` only | Credentials accepted for this chain; ignored for `BEARER`. |

The list binds directly onto a `List<SecurityRuleProperties>` bean via
`@ConfigurationProperties(prefix = "security")` on a `@Bean` factory method — no wrapper object
needed since the YAML root is already a sequence.

## How the rules become filter chains

Spring Security supports multiple `SecurityFilterChain` beans, each scoped to a set of paths via
`securityMatcher(...)` and ordered with `@Order`; the first chain whose matcher matches a request
handles it. `SecurityConfig` groups the configured rules **by `auth-type`**, not one chain per
rule, and declares three fixed beans:

1. **`basicAuthSecurityFilterChain`** (`@Order(1)`) — scoped to every path with `auth-type: BASIC`.
   Enables `httpBasic()` backed by an `InMemoryUserDetailsManager` built from those rules'
   `username`/`password` pairs, and disables CSRF (Basic clients don't carry a session cookie).
2. **`bearerAuthSecurityFilterChain`** (`@Order(2)`) — scoped to every path with `auth-type: BEARER`.
   Enables `oauth2ResourceServer().jwt()`, validated against the same Keycloak issuer as the
   default chain, wired to two explicit beans instead of `Customizer.withDefaults()`:
   - **`bearerTokenResolver`** — how the raw token is pulled off the request. Currently just the
     standard `DefaultBearerTokenResolver` (`Authorization: Bearer <token>` header) made into an
     explicit bean so it's an obvious place to swap in cookie/query-param resolution later.
   - **`jwtAuthenticationConverter`** — how a validated `Jwt` becomes the `Authentication` object
     the rest of Spring Security (and `@PreAuthorize`) sees. Keeps the default `scope`/`scp` →
     `SCOPE_*` authorities and additionally maps Keycloak's `realm_access.roles` claim to
     `ROLE_*` authorities, so `hasRole(...)` works the same way it would against any other
     Spring Security role source.
3. **`defaultSecurityFilterChain`** (`@Order(LOWEST_PRECEDENCE)`) — catch-all for anything not
   listed under `security:`. Unchanged from before this feature: session-based OIDC login for
   browser navigation + Bearer JWT resource server on the same endpoints.

If no rule uses a given `auth-type`, that bean's `@Bean` method returns `null` and Spring simply
skips registering it (a `@Bean` method returning `null` is a supported way to omit a bean
conditionally) — so an empty or absent `security:` list falls back to exactly the original,
single-chain behaviour.

## Enriched `Authentication` (`com.my.craft.security.user`)

Every chain (`basicAuthSecurityFilterChain`, `bearerAuthSecurityFilterChain`,
`defaultSecurityFilterChain`) registers a `UserContextEnrichmentFilter` right after Spring
Security's `AuthorizationFilter` (i.e. only for requests that already authenticated and passed
authorization). It replaces whatever `Authentication` the active mechanism produced with a
`CustomAuthenticationToken` wrapping it, so downstream code (controllers, `@PreAuthorize`, ...)
sees the same authorities/principal as before plus one new thing: `getUserContext()`.

- **`UserService`** (interface) / **`DefaultUserService`** (impl, `@Service`) — the one place that
  knows how to read each principal type (`OidcUser`, `Jwt`, `UserDetails`) into a `UserContext`.
  Extend `DefaultUserService.currentUser(...)` here (e.g. look up more profile data from
  `modules/persistent`) rather than duplicating principal-type switches in every controller.
- **`UserContext`** (record) — `userId`, `username`, `email`, `roles`, and a raw `attributes` map
  for whatever the principal type didn't have a dedicated field for.
- **`CustomAuthenticationToken`** — delegates `getPrincipal()`/`getCredentials()`/`getName()` to
  the original `Authentication` untouched; only `getUserContext()` is new. Existing code reading
  `authentication.getPrincipal()` keeps working unchanged.

`GET /me` (`MeController`) returns `authentication.getUserContext()` directly as a concrete
example of consuming this.

## Known simplifications (template, not hardened)

- **Credentials are pooled per auth-type, not per path.** All `BASIC` rules share one
  `InMemoryUserDetailsManager`, so any configured Basic user can reach *any* `BASIC`-scoped path —
  there is no "this username may only call its own `path`" check. Add a custom
  `AuthenticationProvider`/matcher-aware filter if that's needed.
- **Passwords are stored with `{noop}`** (plain-text compare) since the YAML value is already
  plain text. Swap in a real `PasswordEncoder` (e.g. bcrypt, encoding at startup) before shipping.

## Module dependencies (`modules/security/pom.xml`)

- `spring-boot-starter-oauth2-client` — OIDC login (default chain).
- `spring-boot-starter-oauth2-resource-server` — JWT validation (`BEARER` rules + default chain).
- `spring-web` — `org.springframework.web.cors.*` (CORS config + `CorsUtils.isPreFlightRequest`).
- `jakarta.servlet-api` (`provided`) — `HttpSecurity`/`SecurityFilterChain`/`CorsUtils` reference
  `jakarta.servlet.http.HttpServletRequest`; without this the module fails to compile on its own
  with `cannot access jakarta.servlet.http.HttpServletRequest`. Scope is `provided` because the
  real servlet container comes from whichever app imports this module (e.g. `applications/main`
  via its own `spring-boot-starter-web`) at runtime — this module never runs standalone.
- `org.casbin:jcasbin` — RBAC enforcement engine backing `CasbinConfig` / `CasbinAuthorizationManager`
  (see "RBAC authorization (jcasbin)" below).
- `spring-boot-starter-data-jpa` + `postgresql` (`runtime`) — back `JpaCasbinRuleAdapter`, the
  `casbin_rule` table that makes policy DB-backed instead of file-backed.

## ⚠️ `defaultSecurityFilterChain` must always exist

`FilterChainProxy` only runs the filters of the *first* `SecurityFilterChain` whose
`securityMatcher` matches the request. If a request matches **none** of the registered chains,
Spring Security applies **no filters at all** to it — not a 401, no fallback, the request goes
straight to the controller unauthenticated. `defaultSecurityFilterChain` is what guarantees every
request lands in *some* chain; removing it (or narrowing its `@Order`/matcher) silently turns any
unmatched path into a public endpoint. Keep it registered and last (`Ordered.LOWEST_PRECEDENCE`).

This is also why a rule like `path: /api/*` does **not** protect `/api/admin/test`: Ant/PathPattern
`*` matches exactly one path segment, so `/api/*` only covers `/api/xxx`, not paths one level
deeper. Use `/api/**` to cover a whole subtree.

It also silently breaks CORS, not just auth: `.cors(...)` only takes effect on a request that
matches *some* `SecurityFilterChain` (each chain configures its own `CorsFilter` from
`corsConfigurationSource()`). A path outside every explicit `security:` rule and with no catch-all
chain gets **no CORS headers at all**, so a browser client (e.g. the Vite dev server at
`http://localhost:5173`) has its request blocked even though `app.cors.allowed-origins` is set
correctly — it looks like a CORS misconfiguration but the actual cause is the missing chain.

## RBAC authorization (jcasbin)

`bearerAuthSecurityFilterChain` additionally enforces role-based access control via
[jcasbin](https://github.com/casbin/jcasbin), on top of (not instead of) `authenticated()`:

```java
AuthorizationManager<RequestAuthorizationContext> rbac = AuthorizationManagers.<RequestAuthorizationContext>allOf(
        new AuthorizationDecision(false),
        AuthenticatedAuthorizationManager.authenticated(),
        new CasbinAuthorizationManager(casbinEnforcer));
...
.authorizeHttpRequests(authorize -> authorize
        .requestMatchers(CorsUtils::isPreFlightRequest).permitAll()
        .anyRequest().access(rbac))
```

- **`rbac_model.conf`** (classpath, fixed at startup) — a standard RBAC model: `sub`
  (subject/role), `obj` (resource, matched with `keyMatch2`: `/api/users/*` matches
  `/api/users/123` AND `/api/users/123/roles` — unlike the Ant patterns used elsewhere in this
  module, jcasbin's `*` matches across `/`, i.e. behaves like Ant's `**`), `act` (HTTP method, or
  `*` for any method).
- **Policy itself is DB-backed, not file-backed** — the whole point being that it can change at
  runtime. `CasbinConfig` builds the `Enforcer` from `rbac_model.conf` (still a classpath resource,
  copied to a temp file once at startup since jcasbin's model loader needs a real filesystem path)
  plus a `JpaCasbinRuleAdapter`, backed by the `casbin_rule` table (`CasbinRuleEntity` /
  `CasbinRuleJpaRepository`, `com.my.craft.security.authz.jpa`). That table name/shape (`ptype`,
  `v0`..`v5`) is the schema every official Casbin adapter (Go, Node, the JDBC adapter, ...) uses,
  so any Casbin-aware admin tool can read/write it directly too.
- **`rbac_policy.csv` is a one-time seed, not the live source of policy.** `CasbinConfig` only
  reads it if `casbin_rule` is empty (first boot against a fresh DB) — it parses the CSV with
  jcasbin's own `FileAdapter` into a throwaway `Model`, then writes that through
  `JpaCasbinRuleAdapter.savePolicy(...)` into the table. After that, editing the CSV does nothing;
  change policy via `CasbinPolicyController` (below) or by writing to `casbin_rule` yourself.
- **`p` rows** are the actual grants (`role, path pattern, method`); **`g` rows** assign a role to
  a subject (`g, <username>, <role>`). `CasbinAuthorizationManager` calls
  `enforcer.enforce(subject, object, action)` with `subject` = `CustomAuthenticationToken`'s
  `UserContext.username()`, `object` = `request.getRequestURI()`, `action` =
  `request.getMethod()`.
- **Default is deny.** A `BEARER` path with no matching `p` row (directly, or via no `g` role
  assignment for that subject) is rejected — add a policy row before routing new paths through
  this chain, the same way you'd add a new `security:` rule.
- Only `bearerAuthSecurityFilterChain` is wired to `rbac`; `basicAuthSecurityFilterChain` and
  `defaultSecurityFilterChain` still just check `authenticated()`. Swap their
  `.anyRequest().authenticated()` for `.anyRequest().access(rbac)` the same way if those paths
  need RBAC too — `CasbinAuthorizationManager` requires `CustomAuthenticationToken`, i.e. a chain
  that registers `UserContextEnrichmentFilter` before authorization runs, which all three already
  do.
- `subject` is the raw username, decoupled from Spring's `ROLE_*`/`SCOPE_*` authorities — jcasbin
  roles (the `g` rows) are a separate namespace, assigned via `g` rows, not derived from the JWT's
  `realm_access.roles`. Map JWT roles into `g` rows (or change `CasbinAuthorizationManager` to
  build `subject` from an authority instead of the username) if you want Keycloak to be the source
  of truth for role assignment instead.

### Changing policy at runtime

`CasbinPolicyController` (`com.my.craft.security.authz.web`, itself served at
`/api/admin/casbin/*` — `BEARER`-protected like every `/api/**` path, and RBAC-protected by the
seeded `p, admin, /api/admin/casbin/*, *` row, so only an `admin` subject can reach it):

| Endpoint | Effect |
|---|---|
| `GET /api/admin/casbin/policies` | List every `p` row (`enforcer.getPolicy()`). |
| `POST /api/admin/casbin/policies` | Add a `p` row — `{"role", "pathPattern", "method"}`. |
| `DELETE /api/admin/casbin/policies` | Remove a `p` row (exact match on all three fields). |
| `GET /api/admin/casbin/roles` | List every `g` row (`enforcer.getGroupingPolicy()`). |
| `POST /api/admin/casbin/roles` | Assign a role — `{"username", "role"}`. |
| `DELETE /api/admin/casbin/roles` | Remove a role assignment. |
| `POST /api/admin/casbin/reload` | Re-read `casbin_rule` from the DB. |

Calls through `Enforcer`'s management API (`addPolicy`/`removePolicy`/`addRoleForUser`/...) update
*both* this instance's in-memory copy and the `casbin_rule` table immediately (jcasbin's
Auto-Save feature, on by default) — that's the supported way to change policy without a redeploy.
Writing to `casbin_rule` some other way (psql, a migration, another service) skips the in-memory
side: an already-running `Enforcer` won't see it until something calls `/reload` (or the instance
restarts). Running multiple instances has the same gap between them — jcasbin's `Watcher`
mechanism (e.g. a Redis pub/sub adapter) exists to broadcast "reload" across instances, but isn't
wired up here.

## Extending with a new `auth-type`

1. Add the value to `AuthType`.
2. Add a `@Bean @Order(n) SecurityFilterChain ...` method in `SecurityConfig`, scoped via
   `pathsOf(securityRules, AuthType.YOUR_TYPE)`, following the same "return `null` if empty"
   pattern.
3. Give it an `@Order` between the existing specific chains and `defaultSecurityFilterChain`
   (which must stay last).
