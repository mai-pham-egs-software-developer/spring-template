# `modules/file-storage` — presigned uploads + temp-file cleanup

`FileStorageService` issues a file id and a presigned URL up front; the caller uploads/downloads
object bytes directly against the storage backend, so they never pass through this service.

## Config schema (`file-storage:` in `application.yml`)

```yaml
file-storage:
  bucket: my-bucket
  key-prefix: uploads
  upload:
    expiration: PT15M       # how long a presigned upload URL stays valid
  download:
    expiration: PT10M       # how long a presigned download URL stays valid
  temp:
    ttl: PT24H               # age at which an unconfirmed temp upload becomes eligible for cleanup
    cleanup-cron: "0 0 * * * *"   # when TempFileCleanupScheduler runs
  s3:
    region: us-east-1
    endpoint:                       # unset = real AWS S3; set to switch provider, see below
    path-style-access-enabled: false
    access-key:                     # optional static credentials; falls back to DefaultCredentialsProvider
    secret-key:
```

`applications/main/application.yml` sets these to point at the local MinIO from
`infras/local/compose.yml` **by default** (`endpoint: http://localhost:49000`,
`path-style-access-enabled: true`, `minioadmin`/`minioadmin`) rather than leaving it commented out
-- this is a local-dev template, same as the hard-coded `localhost:8080` Keycloak issuer-uri above
it. Blank out `endpoint`/`path-style-access-enabled`/`access-key`/`secret-key` to target real AWS
S3 instead.

Bound by `FileStorageProperties` (`com.my.craft.filestorage.config`).

## `StorageProvider` — the swappable backend

`storage.StorageProvider` is the only thing `FileStorageService` and `TempFileCleanupScheduler`
depend on for talking to object storage: `presignUpload`, `presignDownload`, `exists`, `delete`.
`storage.S3StorageProvider` is the only implementation, built from the AWS SDK's `S3Client` +
`S3Presigner`.

Because MinIO (and most self-hosted object stores) speak the S3 API, switching provider today is
purely `file-storage.s3.*` config, no code change:

- **AWS S3**: leave `s3.endpoint` unset. Credentials come from the default AWS provider chain
  (env vars, `~/.aws/credentials`, instance/task role, ...) unless `s3.access-key`/`s3.secret-key`
  are set.
- **MinIO** (or another S3-compatible store): set `s3.endpoint` (e.g.
  `http://localhost:49000` for the `minio` service in `infras/local/compose.yml` — its S3 API
  port is exposed on the host as `49000`, console on `49001`), `s3.path-style-access-enabled: true`
  (MinIO doesn't do virtual-hosted `bucket.endpoint` style by default), and
  `s3.access-key`/`s3.secret-key` (`minioadmin`/`minioadmin` for that same local `minio` service).
  This is exactly `applications/main`'s default (see above) — its `my-bucket` bucket is created
  automatically by the one-shot `minio-init` service (`mc mb --ignore-existing local/my-bucket`)
  the first time `docker compose up` runs against `infras/local/compose.yml`.

A genuinely different backend (Azure Blob, GCS, local disk) means writing a new
`StorageProvider` implementation and registering it as the `storageProvider` bean (see
`FileStorageConfig` — `@ConditionalOnMissingBean`-friendly if you want it to coexist with the
default); callers don't change.

## `FileMetadataStore` — where `StoredFile` rows live

`file.FileMetadataStore` is the interface `FileStorageService` and `TempFileCleanupScheduler`
depend on for metadata. `file.jpa.JpaFileMetadataStore` is what's actually wired at runtime —
backed by `file.jpa.StoredFileEntity` / `StoredFileJpaRepository` (Spring Data JPA) against a
`stored_file` table, so metadata is durable and shared across instances of the same service.

- **Datasource**: `applications/main/application.yml`'s `spring.datasource.*` points at a
  dedicated `app` database on the same Postgres container Keycloak uses
  (`infras/local/compose.yml`), *not* Keycloak's own `keycloak` database — keeps `stored_file` out
  of Keycloak's schema. That `app` database is created by `infras/local/init-app-db.sh`, which
  only runs the first time the postgres container's data volume initializes; if the stack was
  already running before this was added, recreate the volume (`docker compose down -v`) or create
  the database by hand.
- **Schema management**: `spring.jpa.hibernate.ddl-auto: update` — Hibernate adds/updates the
  `stored_file` table itself. No Flyway/Liquibase in this repo yet; swap to one before production
  (see "Known simplifications" below).
- **Entity ↔ domain mapping**: `StoredFile` (the domain type `FileStorageService` and callers use)
  has no JPA annotations at all — `JpaFileMetadataStore` converts to/from `StoredFileEntity`
  internally, so nothing outside `file.jpa` needs to know persistence is JPA-based, and swapping
  persistence tech again later only means writing a new `FileMetadataStore` + registering its
  bean, same as `StorageProvider`.
- **`@EntityScan`/`@EnableJpaRepositories`**: unlike plain `@Component` beans, these don't follow
  a consuming app's widened `@SpringBootApplication(scanBasePackages = ...)` — Spring Boot's
  implicit entity/repository scan only covers the *declaring app's own package*
  (`com.my.craft.main`) unless told otherwise. `FileStorageConfig` declares both explicitly,
  scoped to `com.my.craft.filestorage.file.jpa`, so the module works out of the box for any app
  that imports it. One caveat: Boot only applies its *own* implicit default when no `@EntityScan`
  exists anywhere in the context — since this module's is now the only one, an app that later adds
  its own `@Entity` classes directly under its own package will need to broaden this
  `@EntityScan`'s `basePackages` (or add its own) rather than relying on Boot's default.
- `InMemoryFileMetadataStore` still exists but is no longer a `@Component` — it's a plain class
  kept for tests that want a `FileMetadataStore` without a real database (construct it with `new`).

## `FileStorageService` (`com.my.craft.filestorage.service`)

Every method takes the caller's `UserContext` (`com.my.craft.security.user`, see docs/security.md)
as an explicit `actor` argument — this module depends on `modules/security` for that type alone,
not for any auth enforcement of its own (that's still the job of the `security:` rule below).

| Method | What it does |
|---|---|
| `createUploadUrl(CreateUploadRequest, actor)` | Generates a file id, builds the object key (`{key-prefix}/{fileId}-{sanitizedFileName}`), asks the `StorageProvider` for a presigned PUT URL, and records a `PENDING` `StoredFile` with `createdBy = AuditActor.from(actor)`. |
| `confirm(fileId, ConfirmUploadRequest, actor)` | Marks the `StoredFile` `CONFIRMED`, stamps `confirmedBy = AuditActor.from(actor)`, and records the byte size the client observed uploading (the backend never sees the object bytes for a presigned PUT, so the client is the only source of truth for size) — the caller calls this once the upload actually succeeded, exempting it from cleanup. |
| `createDownloadUrl(fileId, actor)` | Presigned GET URL for a previously-uploaded file. Throws `FileNotFoundException` (mapped to `404` via `FileStorageExceptionHandler`) if the id is unknown. |
| `delete(fileId, actor)` | Deletes the object from storage and its metadata. |
| `listFiles(actor)` | Every `CONFIRMED` `StoredFile`, most recently created first, projected to `FileSummaryResponse`. `PENDING` (unconfirmed) files are excluded. |

`CreateUploadRequest.temporary` marks the upload as a **temp file**: one that is expected to be
confirmed quickly (e.g. a staged attachment) and should be purged automatically if it never is.
Non-temporary uploads left `PENDING` are never auto-deleted.

`actor` is only ever logged (`DefaultFileStorageService` emits an info-level log line per
create/confirm/delete/download naming the actor's `userId`) and stamped onto `StoredFile.createdBy`
/`confirmedBy` (`file.AuditActor` — just `userId` + `username`, trimmed from `UserContext`). There
is no per-owner authorization check (any authenticated caller can act on any file id) — add one at
the caller, or in `DefaultFileStorageService`, if files need to be private to their uploader.

## `GET/POST /api/files/**` (`FileStorageController`)

- `GET /api/files` → `[{fileId, fileName, contentType, size, createdAt, uploadedBy}, ...]` — confirmed files only, most recent first.
- `POST /api/files/presign-upload` — body `{fileName, contentType, temporary}` → `{fileId, uploadUrl, key, expiresAt}`.
- `GET /api/files/{fileId}/presign-download` → `{downloadUrl, expiresAt}`.
- `POST /api/files/{fileId}/confirm` — body `{size}` → `204`.
- `DELETE /api/files/{fileId}` → `204`.

`/api/files/**` already falls under the existing `path: /api/**, auth-type: BEARER` rule in
`application.yml`'s `security:` list (see `modules/security`, docs/security.md) — no extra
security config needed when this module is added to a service that already imports `security`.
Every endpoint additionally takes the request's `Authentication`, casts it to
`CustomAuthenticationToken` and reads `getUserContext()` off it (same pattern as `MeController`) to
get the `actor` passed into `FileStorageService`; if that cast fails (the matched chain didn't
register `UserContextEnrichmentFilter`), the endpoint fails loudly with `IllegalStateException`
rather than silently auditing as an unknown user.

## Temp-file cleanup (`schedule.TempFileCleanupScheduler`)

Runs on `file-storage.temp.cleanup-cron` (default: top of every hour). Finds every `StoredFile`
that is `temporary = true`, still `PENDING`, and older than `file-storage.temp.ttl`; deletes the
object from storage and its metadata. A failure on one file is logged and does not stop the rest
of the batch.

## Known simplifications (template, not hardened)

- **Schema managed by `ddl-auto: update`, not a migration tool** — fine while this module is the
  schema's only owner and stays in dev/template use; swap to Flyway/Liquibase (or a
  `modules/persistent`-backed `FileMetadataStore`, once that module exists — nothing else needs to
  change, `FileStorageService` and the scheduler only depend on the `FileMetadataStore` interface)
  before production.
- **No multipart upload support** — `presignUpload` issues a single presigned `PUT`, so it's
  suited to small/medium files a client can upload in one request. Add
  `presignCreateMultipartUpload`/`presignUploadPart` support to `StorageProvider` for large files.
- **`buildKey` sanitizes the filename by regex, not by MIME/extension allowlisting** — this module
  does not validate `contentType` against a whitelist; add that at the caller if arbitrary
  uploads are a concern.

## Module dependencies (`modules/file-storage/pom.xml`)

- `com.my.craft:security` — `UserContext`/`CustomAuthenticationToken` for the audit `actor`. Does
  **not** pull in any auth *enforcement* — a service importing `file-storage` still needs its own
  `security:` rule (or a chain of its own) to actually require authentication on these paths.
- `spring-boot-starter-web` — `@RestController`, `ResponseEntity`.
- `spring-boot-starter-validation` — `@Valid`/`@NotBlank` on `CreateUploadRequest`.
- `spring-boot-starter-data-jpa` — `StoredFileEntity`/`StoredFileJpaRepository`.
- `postgresql` (runtime) — JDBC driver for the datasource in `applications/main/application.yml`.
- `software.amazon.awssdk:s3` (via the `software.amazon.awssdk:bom` import) — `S3Client`,
  `S3Presigner`.
