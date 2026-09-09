# Project Overview

`backend` is a Maven multi-module reactor split into two top-level groups under `source/`:

- **`applications/`** — *service modules*: independently runnable Spring Boot apps (each has a `spring-boot-maven-plugin` build and a `main()` method).
- **`modules/`** — *library modules*: shared code with no `main()` of its own, imported as a regular Maven dependency by one or more `applications/*`.

```text
backend/source/
├─ applications/            (services)
│  ├─ main/                 → runnable service
│  └─ inventory-service/    → runnable service (planned)
└─ modules/                 (libraries)
   ├─ security/             → auth/authz library, owns its own tables
   ├─ persistent/           → shared persistence layer / shared tables
   └─ data-audit-log/       → audit logging library (planned)
```

## `applications/` — service modules

Each module here is a deployable unit: its own `pom.xml` with `spring-boot-starter-parent` as Maven parent, a `@SpringBootApplication` entry point, and the `spring-boot-maven-plugin`. A service module pulls in whichever `modules/*` libraries it needs as plain dependencies.

### `main`

The service run module — the actual application process. It depends on `modules/security` for authentication/authorization and widens `@SpringBootApplication(scanBasePackages = ...)` so Spring also picks up the `@Configuration`/`@Bean` classes that live in the imported library's package instead of its own.

### `inventory-service`

Planned second service module (directory exists, not yet scaffolded). It is expected to follow the same shape as `main`: its own runnable app, importing `security` (and `persistent`, once available) rather than re-implementing auth or persistence.

## `modules/` — library modules

Libraries have no entry point and are never run standalone; they are compiled and installed into the local reactor, then pulled in by `applications/*` via a normal `<dependency>`.

### `security`

Split out of `main` so any service module can reuse it instead of re-implementing auth. Responsibilities:

- **Authentication** — OIDC login (`spring-boot-starter-oauth2-client`) for browser/session flows.
- **Authorization** — JWT resource-server validation (`spring-boot-starter-oauth2-resource-server`) for API/Bearer-token calls, plus the CORS policy guarding both.
- **Owns its own tables** — security-related entities (users/credentials/roles/tokens, depending on what the IdP integration needs locally) live in this module, under its `models` package, rather than in the consuming service.

A service module adopts it purely by adding the `com.my.craft:security` dependency and scanning its base package — no auth code duplicated per service.

### `persistent`

The shared persistence layer. It is meant to own the JPA/DB plumbing and any tables that more than one module needs to read or write — the common ground between `data-audit-log`, `security`, and the `applications/*` service modules, so none of them has to stand up its own copy of shared entities/repositories. Currently an empty placeholder — not yet scaffolded into the reactor.

### `data-audit-log`

Planned audit-logging library. It is expected to depend on `persistent` for its storage rather than owning a separate schema. Currently an empty placeholder — not yet scaffolded into the reactor.

## Dependency direction

```text
applications/main  ──depends on──▶  modules/security  ──depends on──▶  modules/persistent
applications/inventory-service (planned) ─▶ modules/security, modules/persistent
modules/data-audit-log (planned) ────────▶  modules/persistent
```

Libraries never depend on service modules, and `persistent` is the one library every other module (services included) is expected to sit on top of for shared tables — nothing should reimplement its own copy of shared entities.

## Status

| Module | Kind | Status |
|---|---|---|
| `applications/main` | service | implemented, runnable |
| `applications/inventory-service` | service | placeholder |
| `modules/security` | library | implemented (auth/authz config); table models not yet added |
| `modules/persistent` | library | placeholder |
| `modules/data-audit-log` | library | placeholder |

See [../README.md](../README.md) for the full file-level module tree.
