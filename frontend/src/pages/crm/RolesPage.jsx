import { useNavigate } from "react-router-dom";
import { ChevronRight, Plus } from "lucide-react";
import { ROLES } from "../../data/crmMockData";
import { ListPageHeader } from "../../layout/CrmPageHeader";
import { primaryButton, row, tableCard, td, th } from "../../layout/crmStyles";

export function RolesPage() {
  const navigate = useNavigate();

  return (
    <div>
      <ListPageHeader
        breadcrumb="CRM · Administration"
        title="Roles & Permissions"
        searchPlaceholder="Search roles…"
        action={
          <button
            type="button"
            onClick={() => navigate("/roles/new")}
            style={{ ...primaryButton, display: "flex", alignItems: "center", gap: 6 }}
          >
            <Plus size={18} strokeWidth={1.5} />
            New role
          </button>
        }
      />

      <div style={{ ...tableCard, overflowX: "auto" }}>
        <table style={{ minWidth: 720 }}>
          <thead>
            <tr style={{ background: "var(--color-mist)" }}>
              <th style={th}>Role</th>
              <th style={th}>Description</th>
              <th style={th}>Users</th>
              <th style={th}></th>
            </tr>
          </thead>
          <tbody>
            {ROLES.map((r) => (
              <tr key={r.id} onClick={() => navigate(`/roles/${r.id}`)} style={row}>
                <td style={{ ...td, fontWeight: 500 }}>{r.name}</td>
                <td style={{ ...td, color: "var(--color-fg-2)" }}>{r.description}</td>
                <td style={{ ...td, color: "var(--color-fg-2)" }}>{r.userCount}</td>
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
