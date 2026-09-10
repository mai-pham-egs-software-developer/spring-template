import { useEffect, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { Download, Trash2 } from "lucide-react";
import { deleteFile, listFiles, presignDownload } from "../../api/files";
import { BackToListLink, ListPageHeader } from "../../layout/CrmPageHeader";
import { card, deleteButton, grid2, label, readonly, secondaryButton } from "../../layout/crmStyles";
import { formatBytes, formatDateTime, iconForContentType } from "./fileFormat";

export function FileDetailPage() {
  const navigate = useNavigate();
  const { fileId } = useParams();
  const [file, setFile] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [busy, setBusy] = useState(false);

  useEffect(() => {
    setLoading(true);
    setError(null);
    // No single-file GET endpoint exists on the backend (only presign-download/confirm/delete by
    // id) -- list and find by id, same source of truth the Files list page uses.
    listFiles()
      .then((files) => setFile(files.find((f) => f.fileId === fileId) ?? null))
      .catch((err) => setError(err.response?.data?.message ?? err.message))
      .finally(() => setLoading(false));
  }, [fileId]);

  function handleDownload() {
    setBusy(true);
    setError(null);
    presignDownload(fileId)
      .then(({ downloadUrl }) => window.open(downloadUrl, "_blank", "noopener,noreferrer"))
      .catch((err) => setError(err.response?.data?.message ?? err.message))
      .finally(() => setBusy(false));
  }

  function handleDelete() {
    setBusy(true);
    setError(null);
    deleteFile(fileId)
      .then(() => navigate("/files"))
      .catch((err) => {
        setError(err.response?.data?.message ?? err.message);
        setBusy(false);
      });
  }

  if (loading) {
    return (
      <div>
        <ListPageHeader breadcrumb="CRM" title="File Management" action={<BackToListLink to="/files" />} />
        <p>Loading…</p>
      </div>
    );
  }

  if (!file) {
    return (
      <div>
        <ListPageHeader breadcrumb="CRM" title="File Management" action={<BackToListLink to="/files" />} />
        <p>{error ?? "File not found."}</p>
      </div>
    );
  }

  const Icon = iconForContentType(file.contentType);

  return (
    <div>
      <ListPageHeader breadcrumb="CRM" title="File Management" action={<BackToListLink to="/files" />} />
      {error && <p style={{ color: "var(--color-error)" }}>{error}</p>}
      <div style={card}>
        <div style={{ display: "flex", alignItems: "center", gap: 14, marginBottom: 28 }}>
          <div
            style={{
              width: 52,
              height: 52,
              borderRadius: "var(--radius-md)",
              background: "var(--color-mist)",
              color: "var(--color-fg-2)",
              display: "flex",
              alignItems: "center",
              justifyContent: "center",
              flex: "none",
            }}
          >
            <Icon size={26} strokeWidth={1.5} />
          </div>
          <div>
            <div style={{ fontSize: 18, fontWeight: 600 }}>{file.fileName}</div>
            <div style={{ color: "var(--color-fg-3)", fontSize: 13 }}>
              {formatBytes(file.size)} · uploaded by {file.uploadedBy ?? "—"}
            </div>
          </div>
        </div>
        <div style={grid2}>
          <div>
            <div style={label}>File name</div>
            <div style={readonly}>{file.fileName}</div>
          </div>
          <div>
            <div style={label}>Content type</div>
            <div style={readonly}>{file.contentType}</div>
          </div>
          <div>
            <div style={label}>Size</div>
            <div style={readonly}>{formatBytes(file.size)}</div>
          </div>
          <div>
            <div style={label}>Uploaded</div>
            <div style={readonly}>{formatDateTime(file.createdAt)}</div>
          </div>
        </div>
        <div
          style={{
            display: "flex",
            justifyContent: "space-between",
            alignItems: "center",
            marginTop: 32,
            paddingTop: 20,
            borderTop: "1px solid var(--color-border)",
          }}
        >
          <button type="button" onClick={handleDelete} disabled={busy} style={deleteButton}>
            <Trash2 size={18} strokeWidth={1.5} />
            Delete file
          </button>
          <button type="button" onClick={handleDownload} disabled={busy} style={secondaryButton}>
            <Download size={18} strokeWidth={1.5} />
            Download
          </button>
        </div>
      </div>
    </div>
  );
}
