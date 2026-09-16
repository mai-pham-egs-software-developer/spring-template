import { useCallback, useEffect, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { Check, Pencil, Plus, Trash2, X } from "lucide-react";
import {
  addMember,
  createOrganization,
  createRole,
  deleteOrganization,
  deleteRole,
  getOrganization,
  listMembers,
  listRoles,
  removeMember,
  updateMember,
  updateOrganization,
  updateRole,
} from "../../api/organizations";
import { listUsers } from "../../api/users";
import { BackToListLink, ListPageHeader } from "../../layout/CrmPageHeader";
import { card, deleteButton, label, primaryButton, tableCard, td, th } from "../../layout/crmStyles";

const iconButton = {
  display: "inline-flex",
  alignItems: "center",
  background: "none",
  border: "none",
  color: "var(--color-fg-3)",
  cursor: "pointer",
  padding: 6,
};

export function OrganizationDetailPage() {
  const navigate = useNavigate();
  const { organizationId } = useParams();
  const isNew = organizationId === "new";

  const [name, setName] = useState("");
  const [loading, setLoading] = useState(!isNew);
  const [notFound, setNotFound] = useState(false);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState(null);

  const refresh = useCallback(() => {
    if (isNew) return;
    setLoading(true);
    setError(null);
    return getOrganization(organizationId)
      .then((org) => setName(org.name))
      .catch((err) => {
        if (err.response?.status === 404) setNotFound(true);
        else setError(err.response?.data?.detail ?? err.message);
      })
      .finally(() => setLoading(false));
  }, [isNew, organizationId]);

  useEffect(() => {
    refresh();
  }, [refresh]);

  function handleSave() {
    setSaving(true);
    setError(null);
    const request = isNew ? createOrganization({ name }) : updateOrganization(organizationId, { name });
    request
      .then((org) => navigate(isNew ? `/organizations/${org.id}` : "/organizations"))
      .catch((err) => setError(err.response?.data?.detail ?? err.message))
      .finally(() => setSaving(false));
  }

  function handleDelete() {
    setError(null);
    deleteOrganization(organizationId)
      .then(() => navigate("/organizations"))
      .catch((err) => setError(err.response?.data?.detail ?? err.message));
  }

  if (loading) {
    return (
      <div>
        <ListPageHeader breadcrumb="Operator Admin · Administration" title="Organization Management" action={<BackToListLink to="/organizations" />} />
        <p>Loading…</p>
      </div>
    );
  }

  if (notFound) {
    return (
      <div>
        <ListPageHeader breadcrumb="Operator Admin · Administration" title="Organization Management" action={<BackToListLink to="/organizations" />} />
        <p>Organization not found.</p>
      </div>
    );
  }

  return (
    <div>
      <ListPageHeader breadcrumb="Operator Admin · Administration" title="Organization Management" action={<BackToListLink to="/organizations" />} />
      {error && <p style={{ color: "var(--color-error)" }}>{error}</p>}

      <div style={{ ...card, maxWidth: 720, marginBottom: 28 }}>
        <div style={{ fontSize: 18, fontWeight: 600, marginBottom: 20 }}>{isNew ? "New organization" : name}</div>
        <div style={{ marginBottom: isNew ? 0 : 20 }}>
          <div style={label}>Name</div>
          <input type="text" value={name} onChange={(e) => setName(e.target.value)} style={{ width: "100%" }} />
        </div>
        <div
          style={{
            display: "flex",
            justifyContent: isNew ? "flex-end" : "space-between",
            alignItems: "center",
            marginTop: 24,
            paddingTop: 20,
            borderTop: "1px solid var(--color-border)",
          }}
        >
          {!isNew && (
            <button type="button" onClick={handleDelete} style={deleteButton}>
              <Trash2 size={18} strokeWidth={1.5} />
              Delete organization
            </button>
          )}
          <button type="button" onClick={handleSave} disabled={saving || !name.trim()} style={primaryButton}>
            {saving ? "Saving…" : isNew ? "Create organization" : "Save changes"}
          </button>
        </div>
      </div>

      {!isNew && (
        <>
          <RolesSection organizationId={organizationId} />
          <MembersSection organizationId={organizationId} />
        </>
      )}
    </div>
  );
}

function RolesSection({ organizationId }) {
  const [roles, setRoles] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [newName, setNewName] = useState("");
  const [editingId, setEditingId] = useState(null);
  const [editingName, setEditingName] = useState("");

  const refresh = useCallback(() => {
    setLoading(true);
    setError(null);
    return listRoles(organizationId)
      .then(setRoles)
      .catch((err) => setError(err.response?.data?.detail ?? err.message))
      .finally(() => setLoading(false));
  }, [organizationId]);

  useEffect(() => {
    refresh();
  }, [refresh]);

  function handleAdd(event) {
    event.preventDefault();
    if (!newName.trim()) return;
    createRole(organizationId, { name: newName.trim() })
      .then(() => {
        setNewName("");
        return refresh();
      })
      .catch((err) => setError(err.response?.data?.detail ?? err.message));
  }

  function startEdit(role) {
    setEditingId(role.id);
    setEditingName(role.name);
  }

  function saveEdit(roleId) {
    if (!editingName.trim()) return;
    updateRole(organizationId, roleId, { name: editingName.trim() })
      .then(() => {
        setEditingId(null);
        return refresh();
      })
      .catch((err) => setError(err.response?.data?.detail ?? err.message));
  }

  function handleDelete(roleId) {
    deleteRole(organizationId, roleId)
      .then(() => refresh())
      .catch((err) => setError(err.response?.data?.detail ?? err.message));
  }

  return (
    <div style={{ marginBottom: 28 }}>
      <div style={{ fontSize: 15, fontWeight: 600, marginBottom: 12 }}>Roles</div>
      {error && <p style={{ color: "var(--color-error)" }}>{error}</p>}
      <div style={{ ...tableCard, maxWidth: 720 }}>
        <table style={{ width: "100%" }}>
          <thead>
            <tr style={{ background: "var(--color-mist)" }}>
              <th style={th}>Name</th>
              <th style={{ ...th, width: 100 }}></th>
            </tr>
          </thead>
          <tbody>
            {loading && (
              <tr>
                <td style={td} colSpan={2}>
                  Loading…
                </td>
              </tr>
            )}
            {!loading && roles.length === 0 && (
              <tr>
                <td style={td} colSpan={2}>
                  No roles yet.
                </td>
              </tr>
            )}
            {!loading &&
              roles.map((role) => (
                <tr key={role.id}>
                  {editingId === role.id ? (
                    <>
                      <td style={td}>
                        <input
                          type="text"
                          value={editingName}
                          onChange={(e) => setEditingName(e.target.value)}
                          style={{ width: "100%" }}
                          autoFocus
                        />
                      </td>
                      <td style={{ ...td, textAlign: "right", whiteSpace: "nowrap" }}>
                        <button type="button" onClick={() => saveEdit(role.id)} title="Save" style={{ ...iconButton, color: "var(--color-purple)" }}>
                          <Check size={18} strokeWidth={1.5} />
                        </button>
                        <button type="button" onClick={() => setEditingId(null)} title="Cancel" style={iconButton}>
                          <X size={18} strokeWidth={1.5} />
                        </button>
                      </td>
                    </>
                  ) : (
                    <>
                      <td style={{ ...td, fontWeight: 500 }}>{role.name}</td>
                      <td style={{ ...td, textAlign: "right", whiteSpace: "nowrap" }}>
                        <button type="button" onClick={() => startEdit(role)} title="Edit" style={iconButton}>
                          <Pencil size={18} strokeWidth={1.5} />
                        </button>
                        <button
                          type="button"
                          onClick={() => handleDelete(role.id)}
                          title="Delete"
                          style={{ ...iconButton, color: "var(--color-error)" }}
                        >
                          <Trash2 size={18} strokeWidth={1.5} />
                        </button>
                      </td>
                    </>
                  )}
                </tr>
              ))}
          </tbody>
        </table>
      </div>
      <form onSubmit={handleAdd} style={{ display: "flex", gap: 10, marginTop: 12, maxWidth: 720 }}>
        <input type="text" value={newName} onChange={(e) => setNewName(e.target.value)} placeholder="New role name" style={{ flex: 1 }} />
        <button type="submit" disabled={!newName.trim()} style={{ ...primaryButton, display: "flex", alignItems: "center", gap: 6 }}>
          <Plus size={18} strokeWidth={1.5} />
          Add role
        </button>
      </form>
    </div>
  );
}

function MembersSection({ organizationId }) {
  const [members, setMembers] = useState([]);
  const [roles, setRoles] = useState([]);
  const [allUsers, setAllUsers] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  const [newUserId, setNewUserId] = useState("");
  const [newRoleId, setNewRoleId] = useState("");
  const [editingUserId, setEditingUserId] = useState(null);
  const [editingRoleId, setEditingRoleId] = useState("");

  const refresh = useCallback(() => {
    setLoading(true);
    setError(null);
    return Promise.all([listMembers(organizationId), listRoles(organizationId), listUsers()])
      .then(([membersData, rolesData, usersData]) => {
        setMembers(membersData);
        setRoles(rolesData);
        setAllUsers(usersData);
      })
      .catch((err) => setError(err.response?.data?.detail ?? err.message))
      .finally(() => setLoading(false));
  }, [organizationId]);

  useEffect(() => {
    refresh();
  }, [refresh]);

  function handleAdd(event) {
    event.preventDefault();
    if (!newUserId || !newRoleId) return;
    addMember(organizationId, { userId: newUserId, roleId: Number(newRoleId) })
      .then(() => {
        setNewUserId("");
        setNewRoleId("");
        return refresh();
      })
      .catch((err) => setError(err.response?.data?.detail ?? err.message));
  }

  function startEdit(member) {
    setEditingUserId(member.userId);
    setEditingRoleId(String(member.roleId));
  }

  function saveEdit(userId) {
    if (!editingRoleId) return;
    updateMember(organizationId, userId, { roleId: Number(editingRoleId) })
      .then(() => {
        setEditingUserId(null);
        return refresh();
      })
      .catch((err) => setError(err.response?.data?.detail ?? err.message));
  }

  function handleRemove(userId) {
    removeMember(organizationId, userId)
      .then(() => refresh())
      .catch((err) => setError(err.response?.data?.detail ?? err.message));
  }

  function userName(userId) {
    return allUsers.find((u) => u.id === userId)?.name ?? userId;
  }

  const memberUserIds = new Set(members.map((m) => m.userId));
  const availableUsers = allUsers.filter((u) => !memberUserIds.has(u.id));

  return (
    <div>
      <div style={{ fontSize: 15, fontWeight: 600, marginBottom: 12 }}>Members</div>
      {error && <p style={{ color: "var(--color-error)" }}>{error}</p>}
      <div style={{ ...tableCard, maxWidth: 720 }}>
        <table style={{ width: "100%" }}>
          <thead>
            <tr style={{ background: "var(--color-mist)" }}>
              <th style={th}>User</th>
              <th style={th}>Role</th>
              <th style={{ ...th, width: 100 }}></th>
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
            {!loading && members.length === 0 && (
              <tr>
                <td style={td} colSpan={3}>
                  No members yet.
                </td>
              </tr>
            )}
            {!loading &&
              members.map((member) => (
                <tr key={member.userId}>
                  <td style={{ ...td, fontWeight: 500 }}>{member.userName ?? userName(member.userId)}</td>
                  {editingUserId === member.userId ? (
                    <>
                      <td style={td}>
                        <select value={editingRoleId} onChange={(e) => setEditingRoleId(e.target.value)} style={{ width: "100%" }} autoFocus>
                          {roles.map((role) => (
                            <option key={role.id} value={role.id}>
                              {role.name}
                            </option>
                          ))}
                        </select>
                      </td>
                      <td style={{ ...td, textAlign: "right", whiteSpace: "nowrap" }}>
                        <button
                          type="button"
                          onClick={() => saveEdit(member.userId)}
                          title="Save"
                          style={{ ...iconButton, color: "var(--color-purple)" }}
                        >
                          <Check size={18} strokeWidth={1.5} />
                        </button>
                        <button type="button" onClick={() => setEditingUserId(null)} title="Cancel" style={iconButton}>
                          <X size={18} strokeWidth={1.5} />
                        </button>
                      </td>
                    </>
                  ) : (
                    <>
                      <td style={{ ...td, color: "var(--color-fg-2)" }}>{member.roleName}</td>
                      <td style={{ ...td, textAlign: "right", whiteSpace: "nowrap" }}>
                        <button type="button" onClick={() => startEdit(member)} title="Change role" style={iconButton}>
                          <Pencil size={18} strokeWidth={1.5} />
                        </button>
                        <button
                          type="button"
                          onClick={() => handleRemove(member.userId)}
                          title="Remove from organization"
                          style={{ ...iconButton, color: "var(--color-error)" }}
                        >
                          <Trash2 size={18} strokeWidth={1.5} />
                        </button>
                      </td>
                    </>
                  )}
                </tr>
              ))}
          </tbody>
        </table>
      </div>
      <form onSubmit={handleAdd} style={{ display: "flex", gap: 10, marginTop: 12, maxWidth: 720 }}>
        <select value={newUserId} onChange={(e) => setNewUserId(e.target.value)} style={{ flex: 1 }}>
          <option value="">Select a user…</option>
          {availableUsers.map((u) => (
            <option key={u.id} value={u.id}>
              {u.name}
            </option>
          ))}
        </select>
        <select value={newRoleId} onChange={(e) => setNewRoleId(e.target.value)} style={{ flex: 1 }}>
          <option value="">Select a role…</option>
          {roles.map((role) => (
            <option key={role.id} value={role.id}>
              {role.name}
            </option>
          ))}
        </select>
        <button
          type="submit"
          disabled={!newUserId || !newRoleId}
          style={{ ...primaryButton, display: "flex", alignItems: "center", gap: 6, whiteSpace: "nowrap" }}
        >
          <Plus size={18} strokeWidth={1.5} />
          Add member
        </button>
      </form>
    </div>
  );
}
