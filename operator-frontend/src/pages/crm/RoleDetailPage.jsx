import { useNavigate, useParams } from "react-router-dom";
import { Trash2 } from "lucide-react";
import { BLANK_ROLE_MATRIX, ROLES } from "../../data/crmMockData";
import { BackToListLink, ListPageHeader } from "../../layout/CrmPageHeader";
import { card, deleteButton, label, primaryButton, readonly, secondaryButton, td, th } from "../../layout/crmStyles";

const BLANK_ROLE = { name: "", description: "", userCount: 0, matrix: BLANK_ROLE_MATRIX };

export function RoleDetailPage() {
  const navigate = useNavigate();
  const { roleId } = useParams();
  const isNew = roleId === "new";
  const role = isNew ? BLANK_ROLE : ROLES.find((r) => r.id === roleId);

  if (!role) {
    return (
      <div>
        <ListPageHeader breadcrumb="CRM · Administration" title="Roles & Permissions" action={<BackToListLink to="/roles" />} />
        <p>Role not found.</p>
      </div>
    );
  }

  return (
    <div>
      <ListPageHeader breadcrumb="CRM · Administration" title="Roles & Permissions" action={<BackToListLink to="/roles" />} />
      <div style={{ ...card, maxWidth: 720 }}>
        <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: "20px 16px", marginBottom: 28 }}>
          <div>
            <div style={label}>Role name</div>
            <input type="text" defaultValue={role.name} />
          </div>
          <div>
            <div style={label}>Assigned users</div>
            <div style={readonly}>{role.userCount}</div>
          </div>
          <div style={{ gridColumn: "span 2" }}>
            <div style={label}>Description</div>
            <input type="text" defaultValue={role.description} />
          </div>
        </div>
        <div style={{ ...label, marginBottom: 10 }}>Permissions</div>
        <div style={{ border: "1px solid var(--color-border)", borderRadius: "var(--radius-sm)", overflowX: "auto" }}>
          <table style={{ minWidth: 480 }}>
            <thead>
              <tr style={{ background: "var(--color-mist)" }}>
                <th style={th}>Module</th>
                <th style={{ ...th, textAlign: "center" }}>View</th>
                <th style={{ ...th, textAlign: "center" }}>Create</th>
                <th style={{ ...th, textAlign: "center" }}>Edit</th>
                <th style={{ ...th, textAlign: "center" }}>Delete</th>
              </tr>
            </thead>
            <tbody>
              {role.matrix.map((matrixRow) => (
                <tr key={matrixRow.resource}>
                  <td style={{ ...td, fontWeight: 500 }}>{matrixRow.resource}</td>
                  <td style={{ ...td, textAlign: "center" }}>
                    <input type="checkbox" defaultChecked={matrixRow.view} style={{ width: 16, height: 16, accentColor: "var(--color-purple)" }} />
                  </td>
                  <td style={{ ...td, textAlign: "center" }}>
                    <input type="checkbox" defaultChecked={matrixRow.create} style={{ width: 16, height: 16, accentColor: "var(--color-purple)" }} />
                  </td>
                  <td style={{ ...td, textAlign: "center" }}>
                    <input type="checkbox" defaultChecked={matrixRow.edit} style={{ width: 16, height: 16, accentColor: "var(--color-purple)" }} />
                  </td>
                  <td style={{ ...td, textAlign: "center" }}>
                    <input type="checkbox" defaultChecked={matrixRow.delete} style={{ width: 16, height: 16, accentColor: "var(--color-purple)" }} />
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
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
            <Trash2 size={18} strokeWidth={1.5} />
            Delete role
          </button>
          <div style={{ display: "flex", gap: 10 }}>
            <button type="button" onClick={() => navigate("/roles")} style={secondaryButton}>
              Cancel
            </button>
            <button type="button" onClick={() => navigate("/roles")} style={primaryButton}>
              Save changes
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}
