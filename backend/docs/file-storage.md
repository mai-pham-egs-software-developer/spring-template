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
  `http://localhost:9000`), `s3.path-style-access-enabled: true` (MinIO doesn't do virtual-hosted
  `bucket.endpoint` style by default), and `s3.access-key`/`s3.secret-key`.

A genuinely different backend (Azure Blob, GCS, local disk) means writing a new
`StorageProvider` implementation and registering it as the `storageProvider` bean (see
`FileStorageConfig` — `@ConditionalOnMissingBean`-friendly if you want it to coexist with the
default); callers don't change.

## `FileStorageService` (`com.my.craft.filestorage.service`)

Every method takes the caller's `UserContext` (`com.my.craft.security.user`, see docs/security.md)
as an explicit `actor` argument — this module depends on `modules/security` for that type alone,
not for any auth enforcement of its own (that's still the job of the `security:` rule below).

| Method | What it does |
|---|---|
| `createUploadUrl(CreateUploadRequest, actor)` | Generates a file id, builds the object key (`{key-prefix}/{fileId}-{sanitizedFileName}`), asks the `StorageProvider` for a presigned PUT URL, and records a `PENDING` `StoredFile` with `createdBy = AuditActor.from(actor)`. |
| `confirm(fileId, actor)` | Marks the `StoredFile` `CONFIRMED` and stamps `confirmedBy = AuditActor.from(actor)` — the caller calls this once the upload actually succeeded, exempting it from cleanup. |
| `createDownloadUrl(fileId, actor)` | Presigned GET URL for a previously-uploaded file. Throws `FileNotFoundException` (mapped to `404` via `FileStorageExceptionHandler`) if the id is unknown. |
| `delete(fileId, actor)` | Deletes the object from storage and its metadata. |

`CreateUploadRequest.temporary` marks the upload as a **temp file**: one that is expected to be
confirmed quickly (e.g. a staged attachment) and should be purged automatically if it never is.
Non-temporary uploads left `PENDING` are never auto-deleted.

`actor` is only ever logged (`DefaultFileStorageService` emits an info-level log line per
create/confirm/delete/download naming the actor's `userId`) and stamped onto `StoredFile.createdBy`
/`confirmedBy` (`file.AuditActor` — just `userId` + `username`, trimmed from `UserContext`). There
is no per-owner authorization check (any authenticated caller can act on any file id) — add one at
the caller, or in `DefaultFileStorageService`, if files need to be private to their uploader.

## `GET/POST /api/files/**` (`FileStorageController`)

- `POST /api/files/presign-upload` — body `{fileName, contentType, temporary}` → `{fileId, uploadUrl, key, expiresAt}`.
- `GET /api/files/{fileId}/presign-download` → `{downloadUrl, expiresAt}`.
- `POST /api/files/{fileId}/confirm` → `204`.
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

- **Metadata store is in-memory** (`file.InMemoryFileMetadataStore`, a `ConcurrentHashMap`) — lost
  on restart, not shared across replicas. Fine for a single-instance dev/template setup; swap in a
  `modules/persistent`-backed `FileMetadataStore` implementation once that module exists (nothing
  else needs to change — `FileStorageService` and the scheduler only depend on the interface).
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
- `software.amazon.awssdk:s3` (via the `software.amazon.awssdk:bom` import) — `S3Client`,
  `S3Presigner`.
