import apiClient from "./client";

export function listFiles() {
  return apiClient.get("/api/files").then((res) => res.data);
}

export function presignUpload({ fileName, contentType, temporary = false }) {
  return apiClient.post("/api/files/presign-upload", { fileName, contentType, temporary }).then((res) => res.data);
}

export function presignDownload(fileId) {
  return apiClient.get(`/api/files/${fileId}/presign-download`).then((res) => res.data);
}

export function confirmUpload(fileId, { size }) {
  return apiClient.post(`/api/files/${fileId}/confirm`, { size });
}

export function deleteFile(fileId) {
  return apiClient.delete(`/api/files/${fileId}`);
}

// The presigned URL is a fully-qualified, pre-authenticated S3 request -- uploaded with a plain
// fetch, not through apiClient, so the app's Keycloak bearer token is never attached to it (and
// wouldn't mean anything to S3 if it were). The Content-Type header here must match the
// contentType passed to presignUpload exactly, since S3 signs it into the request.
async function uploadToPresignedUrl(uploadUrl, file, contentType) {
  const response = await fetch(uploadUrl, {
    method: "PUT",
    headers: { "Content-Type": contentType },
    body: file,
  });
  if (!response.ok) {
    throw new Error(`Upload to storage failed: ${response.status} ${response.statusText}`);
  }
}

/** Presign, upload the bytes directly to storage, then confirm -- the three-step dance the
 * backend's presigned-upload API expects (see backend/docs/file-storage.md). */
export async function uploadFile(file) {
  const contentType = file.type || "application/octet-stream";
  const { fileId, uploadUrl } = await presignUpload({ fileName: file.name, contentType, temporary: false });
  await uploadToPresignedUrl(uploadUrl, file, contentType);
  await confirmUpload(fileId, { size: file.size });
  return fileId;
}
