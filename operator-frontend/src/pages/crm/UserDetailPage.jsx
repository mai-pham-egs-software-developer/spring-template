import { useNavigate, useParams } from "react-router-dom";
import { UserX } from "lucide-react";
import { USERS } from "../../data/crmMockData";
import { BackToListLink, ListPageHeader } from "../../layout/CrmPageHeader";
import { card, deleteButton, grid2, initialsOf, label, primaryButton, readonly, secondaryButton } from "../../layout/crmStyles";

const BLANK_USER = { name: "", email: "", lastActive: "—" };

export function UserDetailPage() {
  const navigate = useNavigate();
  const { userId } = useParams();
  const isNew = userId === "new";
  const user = isNew ? BLANK_USER : USERS.find((u) => u.id === userId);

  if (!user) {
    return (
      <div>
        <ListPageHeader breadcrumb="CRM · Administration" title="User Management" action={<BackToListLink to="/users" />} />
        <p>User not found.</p>
      </div>
    );
  }

  return (
    <div>
      <ListPageHeader breadcrumb="CRM · Administration" title="User Management" action={<BackToListLink to="/users" />} />
      <div style={card}>
        <div style={{ display: "flex", alignItems: "center", gap: 14, marginBottom: 28 }}>
          <div
            style={{
              width: 52,
              height: 52,
              borderRadius: 999,
              background: "var(--color-purple-100)",
              color: "var(--color-purple-700)",
              display: "flex",
              alignItems: "center",
              justifyContent: "center",
              fontSize: 16,
              fontWeight: 600,
              flex: "none",
            }}
          >
            {isNew ? "+" : initialsOf(user.name)}
          </div>
          <div style={{ fontSize: 18, fontWeight: 600 }}>{isNew ? "New user" : user.name}</div>
        </div>
        <div style={grid2}>
          <div>
            <div style={label}>Full name</div>
            <input type="text" defaultValue={user.name} />
          </div>
          <div>
            <div style={label}>Email</div>
            <input type="email" defaultValue={user.email} />
          </div>
          <div>
            <div style={label}>Last active</div>
            <div style={readonly}>{user.lastActive}</div>
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
          <button type="button" style={deleteButton}>
            <UserX size={18} strokeWidth={1.5} />
            Deactivate user
          </button>
          <div style={{ display: "flex", gap: 10 }}>
            <button type="button" onClick={() => navigate("/users")} style={secondaryButton}>
              Cancel
            </button>
            <button type="button" onClick={() => navigate("/users")} style={primaryButton}>
              Save changes
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}
