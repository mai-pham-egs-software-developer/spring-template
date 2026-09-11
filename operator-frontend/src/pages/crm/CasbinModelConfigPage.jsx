import { useCallback, useEffect, useState } from "react";
import { Save } from "lucide-react";
import { getModelConfig, updateModelConfig } from "../../api/casbinConfig";
import { ListPageHeader } from "../../layout/CrmPageHeader";
import { card, primaryButton, secondaryButton } from "../../layout/crmStyles";

// Backend applies a saved model wholesale via Enforcer#setModel + loadPolicy() (see
// backend/docs/security.md) -- a syntactically broken model fails the PUT instead of getting
// stored, so the error banner below is the one place a bad edit here actually surfaces.
export function CasbinModelConfigPage() {
  const [savedContent, setSavedContent] = useState("");
  const [content, setContent] = useState("");
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState(null);

  const refresh = useCallback(() => {
    setLoading(true);
    setError(null);
    return getModelConfig()
      .then((data) => {
        setSavedContent(data.content);
        setContent(data.content);
      })
      .catch((err) => setError(err.response?.data?.message ?? err.message))
      .finally(() => setLoading(false));
  }, []);

  useEffect(() => {
    refresh();
  }, [refresh]);

  const dirty = content !== savedContent;

  function handleSave() {
    setSaving(true);
    setError(null);
    updateModelConfig(content)
      .then((data) => {
        setSavedContent(data.content);
        setContent(data.content);
      })
      .catch((err) => setError(err.response?.data?.message ?? err.message))
      .finally(() => setSaving(false));
  }

  return (
    <div>
      <ListPageHeader breadcrumb="CRM · Administration" title="Casbin Model Config" />

      {error && <p style={{ color: "var(--color-error)" }}>{error}</p>}

      <div style={{ ...card, maxWidth: 800 }}>
        <textarea
          value={loading ? "Loading…" : content}
          onChange={(e) => setContent(e.target.value)}
          disabled={loading || saving}
          spellCheck={false}
          rows={16}
          style={{
            width: "100%",
            fontFamily: "var(--font-mono, monospace)",
            fontSize: 13,
            lineHeight: 1.5,
            padding: 12,
            border: "1px solid var(--color-border)",
            borderRadius: "var(--radius-sm)",
            resize: "vertical",
            boxSizing: "border-box",
          }}
        />
        <div style={{ display: "flex", justifyContent: "flex-end", gap: 10, marginTop: 20 }}>
          <button type="button" onClick={() => setContent(savedContent)} disabled={!dirty || saving} style={secondaryButton}>
            Discard changes
          </button>
          <button
            type="button"
            onClick={handleSave}
            disabled={!dirty || saving || loading}
            style={{ ...primaryButton, display: "flex", alignItems: "center", gap: 6 }}
          >
            <Save size={18} strokeWidth={1.5} />
            {saving ? "Saving…" : "Save"}
          </button>
        </div>
      </div>
    </div>
  );
}
