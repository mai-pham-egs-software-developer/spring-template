import { useCallback, useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { Building2, ChevronRight } from "lucide-react";
import { listMyOrganizations } from "../api/organizations";
import { useOrg } from "../org/OrgContext";
import { card, primaryButton, row } from "../layout/crmStyles";

// Sits between login and the main app: RequireOrg sends anyone with no org selected yet here.
// Picking one persists it (OrgContext -> sessionStorage) and moves on to /profile -- "main bizz
// function" -- from which point every backend call carries X-Org-Id (see api/client.js).
export function SelectOrgPage() {
  const navigate = useNavigate();
  const { selectOrg } = useOrg();
  const [organizations, setOrganizations] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  const refresh = useCallback(() => {
    setLoading(true);
    setError(null);
    return listMyOrganizations()
      .then(setOrganizations)
      .catch((err) => setError(err.response?.data?.detail ?? err.message))
      .finally(() => setLoading(false));
  }, []);

  useEffect(() => {
    refresh();
  }, [refresh]);

  function handleSelect(organization) {
    selectOrg(organization.id);
    navigate("/profile", { replace: true });
  }

  return (
    <div
      style={{
        minHeight: "100vh",
        display: "flex",
        alignItems: "center",
        justifyContent: "center",
        background: "var(--color-mist)",
        fontFamily: "var(--font-sans)",
      }}
    >
      <div style={{ ...card, maxWidth: 440, width: "100%" }}>
        <div style={{ fontSize: 20, fontWeight: 600, marginBottom: 4 }}>Select an organization</div>
        <p style={{ fontSize: 14, color: "var(--color-fg-2)", marginTop: 0, marginBottom: 24 }}>
          Choose which organization to work in.
        </p>

        {error && <p style={{ color: "var(--color-error)" }}>{error}</p>}
        {loading && <p style={{ color: "var(--color-fg-2)" }}>Loading…</p>}
        {!loading && !error && organizations.length === 0 && (
          <p style={{ color: "var(--color-fg-2)" }}>You don't belong to any organization yet.</p>
        )}

        {!loading &&
          organizations.map((organization) => (
            <div
              key={organization.id}
              onClick={() => handleSelect(organization)}
              style={{
                ...row,
                display: "flex",
                alignItems: "center",
                gap: 12,
                padding: "14px 16px",
                marginBottom: 8,
                borderRadius: "var(--radius-sm)",
                border: "1px solid var(--color-border)",
              }}
            >
              <Building2 size={20} strokeWidth={1.5} style={{ color: "var(--color-purple)", flex: "none" }} />
              <span style={{ flex: 1, fontWeight: 500 }}>{organization.name}</span>
              <ChevronRight size={18} strokeWidth={1.5} style={{ color: "var(--color-fg-3)" }} />
            </div>
          ))}

        {!loading && organizations.length > 0 && (
          <button
            type="button"
            onClick={refresh}
            style={{ ...primaryButton, background: "none", color: "var(--color-fg-2)", padding: "8px 0", marginTop: 8 }}
          >
            Refresh list
          </button>
        )}
      </div>
    </div>
  );
}
