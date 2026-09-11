import { useNavigate } from "react-router-dom";
import { ChevronRight, Plus } from "lucide-react";
import { USERS } from "../../data/crmMockData";
import { ListPageHeader } from "../../layout/CrmPageHeader";
import { initialsOf, primaryButton, row, tableCard, td, th } from "../../layout/crmStyles";

export function UsersPage() {
  const navigate = useNavigate();

  return (
    <div>
      <ListPageHeader
        breadcrumb="CRM · Administration"
        title="User Management"
        searchPlaceholder="Search users…"
        action={
          <button
            type="button"
            onClick={() => navigate("/users/new")}
            style={{ ...primaryButton, display: "flex", alignItems: "center", gap: 6 }}
          >
            <Plus size={18} strokeWidth={1.5} />
            New user
          </button>
        }
      />

      <div style={{ ...tableCard, overflowX: "auto" }}>
        <table style={{ minWidth: 720 }}>
          <thead>
            <tr style={{ background: "var(--color-mist)" }}>
              <th style={th}>Name</th>
              <th style={th}>Email</th>
              <th style={th}>Last active</th>
              <th style={th}></th>
            </tr>
          </thead>
          <tbody>
            {USERS.map((u) => (
              <tr key={u.id} onClick={() => navigate(`/users/${u.id}`)} style={row}>
                <td style={td}>
                  <div style={{ display: "flex", alignItems: "center", gap: 10 }}>
                    <div
                      style={{
                        width: 30,
                        height: 30,
                        borderRadius: 999,
                        background: "var(--color-purple-100)",
                        color: "var(--color-purple-700)",
                        display: "flex",
                        alignItems: "center",
                        justifyContent: "center",
                        fontSize: 11,
                        fontWeight: 600,
                        flex: "none",
                      }}
                    >
                      {initialsOf(u.name)}
                    </div>
                    <span style={{ fontWeight: 500, color: "var(--color-fg-1)" }}>{u.name}</span>
                  </div>
                </td>
                <td style={{ ...td, color: "var(--color-fg-2)" }}>{u.email}</td>
                <td style={{ ...td, color: "var(--color-fg-2)" }}>{u.lastActive}</td>
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
