import { useCallback, useEffect, useRef, useState } from "react";
import { useNavigate } from "react-router-dom";
import { ChevronRight, Upload } from "lucide-react";
import { listFiles, uploadFile } from "../../api/files";
import { ListPageHeader } from "../../layout/CrmPageHeader";
import { primaryButton, row, tableCard, td, th } from "../../layout/crmStyles";
import { formatBytes, formatDateTime, iconForContentType } from "./fileFormat";

export function FilesPage() {
  const navigate = useNavigate();
  const fileInputRef = useRef(null);
  const [files, setFiles] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [uploading, setUploading] = useState(false);

  const refresh = useCallback(() => {
    setLoading(true);
    setError(null);
    return listFiles()
      .then(setFiles)
      .catch((err) => setError(err.response?.data?.message ?? err.message))
      .finally(() => setLoading(false));
  }, []);

  useEffect(() => {
    refresh();
  }, [refresh]);

  function handleFileChosen(event) {
    const file = event.target.files?.[0];
    event.target.value = ""; // allow re-selecting the same file next time
    if (!file) return;

    setUploading(true);
    setError(null);
    uploadFile(file)
      .then(() => refresh())
      .catch((err) => setError(err.response?.data?.message ?? err.message))
      .finally(() => setUploading(false));
  }

  return (
    <div>
      <ListPageHeader
        breadcrumb="Operator Admin"
        title="File Management"
        searchPlaceholder="Search files…"
        action={
          <>
            <input ref={fileInputRef} type="file" onChange={handleFileChosen} style={{ display: "none" }} />
            <button
              type="button"
              onClick={() => fileInputRef.current?.click()}
              disabled={uploading}
              style={{ ...primaryButton, display: "flex", alignItems: "center", gap: 6 }}
            >
              <Upload size={18} strokeWidth={1.5} />
              {uploading ? "Uploading…" : "Upload file"}
            </button>
          </>
        }
      />

      {error && <p style={{ color: "var(--color-error)" }}>{error}</p>}

      <div style={tableCard}>
        <table>
          <thead>
            <tr style={{ background: "var(--color-mist)" }}>
              <th style={th}>Name</th>
              <th style={th}>Size</th>
              <th style={th}>Uploaded by</th>
              <th style={th}>Uploaded</th>
              <th style={th}></th>
            </tr>
          </thead>
          <tbody>
            {loading && (
              <tr>
                <td style={td} colSpan={5}>
                  Loading…
                </td>
              </tr>
            )}
            {!loading && files.length === 0 && (
              <tr>
                <td style={td} colSpan={5}>
                  No files uploaded yet.
                </td>
              </tr>
            )}
            {!loading &&
              files.map((f) => {
                const Icon = iconForContentType(f.contentType);
                return (
                  <tr key={f.fileId} onClick={() => navigate(`/files/${f.fileId}`)} style={row}>
                    <td style={td}>
                      <div style={{ display: "flex", alignItems: "center", gap: 10 }}>
                        <Icon size={18} strokeWidth={1.5} style={{ color: "var(--color-fg-3)" }} />
                        <span style={{ fontWeight: 500, color: "var(--color-fg-1)" }}>{f.fileName}</span>
                      </div>
                    </td>
                    <td style={{ ...td, color: "var(--color-fg-2)" }}>{formatBytes(f.size)}</td>
                    <td style={{ ...td, color: "var(--color-fg-2)" }}>{f.uploadedBy ?? "—"}</td>
                    <td style={{ ...td, color: "var(--color-fg-2)" }}>{formatDateTime(f.createdAt)}</td>
                    <td style={{ ...td, textAlign: "right" }}>
                      <ChevronRight size={18} strokeWidth={1.5} style={{ color: "var(--color-fg-3)" }} />
                    </td>
                  </tr>
                );
              })}
          </tbody>
        </table>
      </div>
    </div>
  );
}
