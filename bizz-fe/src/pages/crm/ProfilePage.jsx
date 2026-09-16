import { useAuth } from "../../auth/AuthContext";
import { ListPageHeader } from "../../layout/CrmPageHeader";
import { card, grid2, initialsOf, label, pill, primaryButton, readonly } from "../../layout/crmStyles";

export function ProfilePage() {
  const { user, hasRole } = useAuth();
  const name = user?.name ?? user?.preferred_username ?? "Unknown";
  const email = user?.email ?? "—";
  const phone = user?.phone_number ?? "—";
  const status = "Active";

  return (
    <div>
      <ListPageHeader breadcrumb="Operator Admin" title="My Profile" />
      <div style={card}>
        <div style={{ display: "flex", alignItems: "center", gap: 14, marginBottom: 28 }}>
          <div
            style={{
              width: 52,
              height: 52,
              borderRadius: 999,
              background: "var(--color-blue-100)",
              color: "var(--color-blue-700)",
              display: "flex",
              alignItems: "center",
              justifyContent: "center",
              fontSize: 16,
              fontWeight: 600,
              flex: "none",
            }}
          >
            {initialsOf(name)}
          </div>
          <div>
            <div style={{ fontSize: 18, fontWeight: 600 }}>{name}</div>
            <span style={pill("var(--color-success-bg)", "var(--color-success)")}>{status}</span>
          </div>
        </div>
        <div style={grid2}>
          <div>
            <div style={label}>Full name</div>
            <input type="text" defaultValue={name} />
          </div>
          <div>
            <div style={label}>Email</div>
            <input type="email" defaultValue={email} />
          </div>
          <div>
            <div style={label}>Phone</div>
            <input type="tel" defaultValue={phone} />
          </div>
          <div>
            <div style={label}>Status</div>
            <select defaultValue={status}>
              <option>Active</option>
              <option>Inactive</option>
            </select>
          </div>
          <div>
            <div style={label}>Role</div>
            <div style={readonly}>{hasRole("admin") ? "Administrator" : "Member"}</div>
          </div>
          <div>
            <div style={label}>Username</div>
            <div style={readonly}>{user?.preferred_username ?? "—"}</div>
          </div>
        </div>
        <div
          style={{
            display: "flex",
            justifyContent: "flex-end",
            marginTop: 32,
            paddingTop: 20,
            borderTop: "1px solid var(--color-border)",
          }}
        >
          <button type="button" style={primaryButton}>
            Save changes
          </button>
        </div>
      </div>
    </div>
  );
}
