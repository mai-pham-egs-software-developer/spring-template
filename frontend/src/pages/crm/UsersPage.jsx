import { useCallback, useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { ChevronRight, Plus } from "lucide-react";
import { listUsers } from "../../api/users";
import { ListPageHeader } from "../../layout/CrmPageHeader";
import { initialsOf, primaryButton, row, tableCard, td, th } from "../../layout/crmStyles";
import { UserStatusPill } from "./UserStatusPill";

export function UsersPage() {
  const navigate = useNavigate();
  const [users, setUsers] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  const refresh = useCallback(() => {
    setLoading(true);
    setError(null);
    return listUsers()
      .then(setUsers)
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

      {error && <p style={{ color: "var(--color-error)" }}>{error}</p>}

      <div style={{ ...tableCard, overflowX: "auto" }}>
        <table style={{ minWidth: 640 }}>
          <thead>
            <tr style={{ background: "var(--color-mist)" }}>
              <th style={th}>Name</th>
              <th style={th}>Username</th>
              <th style={th}>Status</th>
              <th style={th}></th>
            </tr>
          </thead>
          <tbody>
            {loading && (
              <tr>
                <td style={td} colSpan={4}>
                  Loading…
                </td>
              </tr>
            )}
            {!loading && users.length === 0 && (
              <tr>
                <td style={td} colSpan={4}>
                  No users yet.
                </td>
              </tr>
            )}
            {!loading &&
              users.map((u) => (
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
                  <td style={{ ...td, color: "var(--color-fg-2)" }}>{u.username}</td>
                  <td style={td}>
                    <UserStatusPill status={u.status} />
                  </td>
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
