import { useCallback, useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { ChevronRight, Plus } from "lucide-react";
import { listOrganizations } from "../../api/organizations";
import { ListPageHeader } from "../../layout/CrmPageHeader";
import { primaryButton, row, tableCard, td, th } from "../../layout/crmStyles";

export function OrganizationsPage() {
  const navigate = useNavigate();
  const [organizations, setOrganizations] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  const refresh = useCallback(() => {
    setLoading(true);
    setError(null);
    return listOrganizations()
      .then(setOrganizations)
      .catch((err) => setError(err.response?.data?.detail ?? err.message))
      .finally(() => setLoading(false));
  }, []);

  useEffect(() => {
    refresh();
  }, [refresh]);

  return (
    <div>
      <ListPageHeader
        breadcrumb="Operator Admin · Administration"
        title="Organization Management"
        searchPlaceholder="Search organizations…"
        action={
          <button
            type="button"
            onClick={() => navigate("/organizations/new")}
            style={{ ...primaryButton, display: "flex", alignItems: "center", gap: 6 }}
          >
            <Plus size={18} strokeWidth={1.5} />
            New organization
          </button>
        }
      />

      {error && <p style={{ color: "var(--color-error)" }}>{error}</p>}

      <div style={tableCard}>
        <table>
          <thead>
            <tr style={{ background: "var(--color-mist)" }}>
              <th style={th}>Name</th>
              <th style={th}>ID</th>
              <th style={th}></th>
            </tr>
          </thead>
          <tbody>
            {loading && (
              <tr>
                <td style={td} colSpan={3}>
                  Loading…
                </td>
              </tr>
            )}
            {!loading && organizations.length === 0 && (
              <tr>
                <td style={td} colSpan={3}>
                  No organizations yet.
                </td>
              </tr>
            )}
            {!loading &&
              organizations.map((org) => (
                <tr key={org.id} onClick={() => navigate(`/organizations/${org.id}`)} style={row}>
                  <td style={{ ...td, fontWeight: 500 }}>{org.name}</td>
                  <td style={{ ...td, color: "var(--color-fg-2)" }}>{org.id}</td>
                  <td style={{ ...td, textAlign: "right" }}>
                    <ChevronRight size={18} strokeWidth={1.5} style={{ color: "var(--color-fg-3)" }} />
                  </td>
                </tr>
              ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}
