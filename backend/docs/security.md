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
- `org.casbin:casbin-spring-boot-starter` — builds the jcasbin `Enforcer`/`Adapter` beans backing
  `CasbinAuthorizationManager` from the `casbin:` block in `application.yml` (see "RBAC
  authorization (jcasbin)" below); brings jcasbin itself transitively.
- `spring-boot-starter-data-jpa` + `postgresql` (`runtime`) — the JDBC `Adapter` reuses this
  module's own Postgres `DataSource` for the `casbin_rule` table; also backs the unrelated
  `User`/`Organization`/`Role`/`Permission` entities (`com.my.craft.security.domain`).

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

- **`rbac_model.conf`** (classpath, `casbin.model` in `application.yml`) — an **org-scoped
  ("domain") RBAC model**, not the plain `sub, obj, act` one this started as:

  ```
  [request_definition]
  r = userId, orgId, objectType, objectId, action

  [policy_definition]
  p = role, orgId, objectType, objectId, action

  [role_definition]
  g = _, _, _

  [policy_effect]
  e = some(where (p.eft == allow))

  [matchers]
  m = (p.role == "PUBLIC" || g(r.userId, p.role, r.orgId)) && (p.orgId == "*" || r.orgId == p.orgId) &&
      (p.objectType == "*" || p.objectType == r.objectType || keyMatch2(r.objectType, p.objectType)) &&
      (p.objectId == "*" || p.objectId == r.objectId) && (p.action == "*" || p.action == r.action)
  ```

  `g = _, _, _` (three tokens, not two) is jcasbin's built-in domain/tenant-scoped RBAC: a `g`
  row means "this user holds this role *within this org*", not globally, and `g(r.userId,
  p.role, r.orgId)` only matches a role the caller actually holds in the org the *request* names
  -- see `MASTER_ID`/`SUPER_ADMIN` below for what that means for a role meant to apply everywhere.
  `keyMatch2` stays in the `objectType` clause (alongside plain equality and the `"*"` wildcard)
  specifically so unannotated handlers (see below), which fall back to a raw request *path* as
  `objectType`, can still be granted with a wildcard pattern like `/operators/casbin/*`, the same
  way the old model's `obj` matching worked.
  - **`p.role == "PUBLIC"`** is the one way to grant a `p` row to *every* caller regardless of any
    `g` role assignment: `g(...)` normally requires an exact-match grouping row for that specific
    `userId`, so there's no built-in wildcard subject otherwise. A row like `p, PUBLIC, *, user,
    <id>, READ` grants that exact `objectType`/`objectId`/`action` to anyone, in any org -- it
    still respects every other field, it only skips the role-membership check. `PUBLIC` isn't a
    real role anyone is ever assigned via `g`; it's purely a sentinel the matcher special-cases.
- **The model itself is also DB-backed past startup**, table `casbin_model_config`
  (`CasbinModelConfig`/`CasbinModelConfigJpaRepository`, `com.my.craft.security.domain.operator`/
  `repository`). `CasbinModelConfigInitializer` (`com.my.craft.security.authz`, an
  `ApplicationRunner`) seeds that table from `rbac_model.conf` on first boot; on every later boot
  it instead parses the stored row and applies it to the `Enforcer` via `setModel` + `loadPolicy()`
  — so a model edited through `CasbinPolicyController`'s `/config` endpoint survives a restart,
  and `rbac_model.conf` only matters for the very first boot against an empty DB. **If a DB
  already has an old-shape row here from before this change, it has to be updated/deleted by
  hand** — the initializer only seeds an empty table, it never overwrites an existing row.
- **Policy itself is DB-backed, not file-backed** — the whole point being that it can change at
  runtime. casbin-spring-boot-starter builds the `Enforcer`/`Adapter` beans from the `casbin:`
  block in `application.yml` (`store-type: jdbc`, `table-name: casbin_rule`,
  `initialize-schema: create`), reusing this module's own Postgres `DataSource`. That table
  name/shape (`ptype`, `v0`..`v5`) is the schema every official Casbin adapter (Go, Node, the JDBC
  adapter, ...) uses, so any Casbin-aware admin tool can read/write it directly too. With the
  5-field `p`/3-field `g` above, a `p` row uses `v0`..`v4` (`role, orgId, objectType, objectId,
  action`) and a `g` row uses `v0`..`v2` (`userId, role, orgId`).
- **There is no policy seed file, but the master account seeds its own grant.** `casbin_rule`
  starts completely empty on a fresh DB — nothing parses a bundled CSV into it (there used to be
  a `CasbinPolicySeeder` doing exactly that; it was removed so the database is the *only* source
  of policy, with no file-based fallback). That would normally mean a fresh environment starts in
  **default-deny for every `BEARER` path, including `CasbinPolicyController` itself**, with no way
  to reach the management API to add the first rows. `MasterAccountInitializer`
  (`com.my.craft.security.service.initial`) breaks that deadlock: alongside the `SUPER_ADMIN`
  `Role`/wildcard `Permission`/`UserOrganizationRole` rows it creates in the plain JPA tables
  (`com.my.craft.security.domain` -- which `CasbinAuthorizationManager` never reads), it also
  inserts the matching `casbin_rule` rows directly through the `Enforcer` (idempotently, via
  `hasGroupingPolicy`/`hasPolicy` checks first): `g, <masterUserId>, SUPER_ADMIN, 0` and `p,
  SUPER_ADMIN, 0, *, *, *`. So on every boot, the master account can reach any RBAC-protected path
  scoped to the master org (`Organization.MASTER_ID`) -- which, per the `@RequiresPermission`
  fallback rule above, is every unannotated path too. Anything else (a second admin, a
  non-platform org's own rows) still needs a manual `INSERT` against `casbin_rule` (`psql`/any
  client), then `POST /operators/casbin/reload` (or a restart) so the running `Enforcer` picks it
  up. From then on, manage policy through `CasbinPolicyController` (below).
- **`p` rows** are the actual grants (`role, orgId, objectType, objectId, action`); **`g` rows**
  assign a role to a user within one org (`userId, role, orgId`). `CasbinAuthorizationManager`
  calls `enforcer.enforce(userId, orgId, objectType, objectId, action)` with `userId` =
  `CustomAuthenticationToken`'s `UserContext.userId()` (the Keycloak subject claim, the same id
  `User.id` uses -- *not* the username). `orgId`/`objectType`/`objectId`/`action` come from one
  of two places:
  - **`@RequiresPermission`** (`com.my.craft.security.annotation`), on the resolved controller
    method or (as a fallback default) its class -- the two are never merged field-by-field, so a
    method that overrides anything must repeat every attribute it needs:
    - `objectType = resource()`, `action = action()`.
    - `objectId` = the `@PathVariable` named by `idParam()`, or `""` for a collection-level
      operation with no single instance.
    - `orgId` = the `@PathVariable` named by `orgIdParam()`, if that's set. **"Master-less"**
      calls -- `orgIdParam` unset (a platform-level operation with no owning org in its path,
      e.g. managing organizations or users themselves) -- fall back to the `X-Org-Id` request
      header if the caller sent one, then only to `Organization.MASTER_ID` (`"0"`) if that's
      absent too (`CasbinAuthorizationManager.ORG_ID_HEADER`). So a client working within some
      org can still get org-scoped permission checks on an endpoint whose path itself carries no
      org id, by sending `X-Org-Id: <id>`; a client that sends nothing keeps the old
      master-org-only behavior.

    This is what `UserController`/`OrganizationController`/`RoleController` use, e.g. `orgId =
    "1"`, `objectType = "role"`, `objectId = "7"`, `action = "WRITE"` for `PUT
    /operators/organizations/1/roles/7` -- matching `Permission.resource`/`Permission.actionType`'s
    own shape, not the URL.
  - Otherwise (no `@RequiresPermission` anywhere on that handler, e.g. `CasbinPolicyController`,
    `MeController`): `objectType = request.getRequestURI()`, `objectId = ""`, `action =
    request.getMethod()`, `orgId` resolved the same "master-less" way as above (`X-Org-Id`, else
    `Organization.MASTER_ID`) -- the original path/method-based behavior, just also scoped for
    the domain-RBAC `g` check.

    `CasbinAuthorizationManager` resolves which handler will run via `RequestMappingHandlerMapping`
    (the same lookup Spring MVC itself does, just done a second time from the security filter,
    since `AuthorizationFilter` runs before `DispatcherServlet`) -- opt-in per controller/method,
    not a breaking change for anything that doesn't use the annotation.
- **`Organization.MASTER_ID`** (`"0"`, `com.my.craft.security.domain.Organization`) is the last
  fallback for a "master-less" call (see above) -- the same org `MasterAccountInitializer`
  creates its `SUPER_ADMIN` role in. A role assignment (`g` row) is scoped to one specific org
  (jcasbin's built-in domain-RBAC has no cross-domain wildcard for `g` itself), so a role meant
  to reach *every* org needs either a `g` row per org, or (for the `p` side only) a policy row
  with `orgId = "*"` shared by several per-org `g` assignments of the same role name.
- **Default is deny.** A `BEARER` path with no matching `p` row (directly, or via no `g` role
  assignment for that user in that org) is rejected — add a policy row before routing new paths
  through this chain, the same way you'd add a new `security:` rule.
- **`casbin.rbac-exempt-paths`** (Ant-style patterns, e.g. `/operators/health/**`) skip
  `CasbinAuthorizationManager` outright — `check()` returns granted before even resolving the
  handler or building the request tuple, for a path that needs to sit under a `BEARER`-protected
  prefix (`/operators/**`, `/biz/**`, `/api/**`) but should behave like `/me`: no org/role/
  permission check, just plain authentication (still enforced separately by
  `AuthenticatedAuthorizationManager.authenticated()` alongside this manager in `SecurityConfig`'s
  `allOf(...)`). `/me` itself doesn't need to be listed here — it isn't matched by any `BEARER`
  path in the `security:` list, so it never reaches this chain at all; this is only for something
  that *does* sit under one of those prefixes. Empty by default. Current entries:
  - `/biz/organizations/me` (`OrganizationController.listMine`) — the caller's own orgs, `userId`
    read from the authenticated token rather than a path variable, so it's inherently self-scoped
    and safe to open to everyone.
  - `/biz/users/*/organizations` (`OrganizationUserController.listOrganizationsOfUser`) — the same
    lookup for an arbitrary `{userId}` path segment; any authenticated caller can query *any*
    user's org list, not just their own. Prefer the `/organizations/me` endpoint above for
    "what orgs am I in" (e.g. bizz-fe's Select Organization screen); this one stays only for
    callers that actually need another user's orgs.
- Only `bearerAuthSecurityFilterChain` is wired to `rbac`; `basicAuthSecurityFilterChain` and
  `defaultSecurityFilterChain` still just check `authenticated()`. Swap their
  `.anyRequest().authenticated()` for `.anyRequest().access(rbac)` the same way if those paths
  need RBAC too — `CasbinAuthorizationManager` requires `CustomAuthenticationToken`, i.e. a chain
  that registers `UserContextEnrichmentFilter` before authorization runs, which all three already
  do.
- `userId` is the raw Keycloak subject claim, decoupled from Spring's `ROLE_*`/`SCOPE_*`
  authorities — jcasbin roles (the `g` rows) are a separate namespace, assigned via `g` rows, not
  derived from the JWT's `realm_access.roles`. Map JWT roles into `g` rows (or change
  `CasbinAuthorizationManager` to build the request tuple from an authority instead) if you want
  Keycloak to be the source of truth for role assignment instead.

### Changing policy at runtime

`CasbinPolicyController` (`com.my.craft.security.web.operator`, itself served at
`/operators/casbin/*` — `BEARER`-protected like every `/operators/**` path, and RBAC-protected
like anything else under this chain: reaching it needs a matching `p` row already in
`casbin_rule` for the caller's `userId`/`orgId`, e.g. `p, admin, 0, /operators/casbin/*, , *`
(five `v` columns: `role, orgId, objectType, objectId, action`) plus a `g, <userId>, admin, 0`
row — see "There is no policy seed file" above for how those rows get there on a fresh DB.

**Policy rows are dynamic by `ptype`**, not one endpoint per row shape — a `ptype` starting with
`g` (role/grouping assertions) is dispatched to jcasbin's `*GroupingPolicy` methods, anything else
(`p`, or a further section like `p2` if `rbac_model.conf` ever grows one) to its `*Policy` methods,
mirroring the `p`/`g` split jcasbin itself uses internally:

| Endpoint | Effect |
|---|---|
| `GET /operators/casbin/policies?ptype=p` | List every row of that `ptype` (`ptype` defaults to `p`; use `g` for role assignments). |
| `POST /operators/casbin/policies` | Add a row — `{"ptype", "params": [...]}` (e.g. `{"ptype":"p","params":["admin","0","/x/*","","GET"]}` or `{"ptype":"g","params":["<userId>","admin","0"]}`). |
| `PUT /operators/casbin/policies` | Replace one row's params — `{"ptype", "oldParams": [...], "newParams": [...]}`. |
| `DELETE /operators/casbin/policies` | Remove a row (exact match on `ptype` + `params`). |
| `POST /operators/casbin/reload` | Re-read `casbin_rule` from the DB. |
| `GET /operators/casbin/config` | Read the current RBAC model definition (the `userId, orgId, objectType, objectId, action` / matcher text), from table `casbin_model_config`. |
| `PUT /operators/casbin/config` | Replace it — `{"content": "..."}`. Parses into a fresh `Model`, swaps it into the running `Enforcer` (`setModel` + `loadPolicy()`), then persists — a syntactically broken model fails the request instead of getting stored. |

Calls through `Enforcer`'s management API (`addNamedPolicy`/`removeNamedPolicy`/
`addNamedGroupingPolicy`/...) update *both* this instance's in-memory copy and the `casbin_rule`
table immediately (jcasbin's Auto-Save feature, on by default) — that's the supported way to
change policy without a redeploy.
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
