import { useCallback, useEffect, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { Trash2 } from "lucide-react";
import { createUser, deleteUser, getUser, updateUser } from "../../api/users";
import { BackToListLink, ListPageHeader } from "../../layout/CrmPageHeader";
import { card, deleteButton, grid2, initialsOf, label, primaryButton, readonly, secondaryButton } from "../../layout/crmStyles";

export function UserDetailPage() {
  const navigate = useNavigate();
  const { userId } = useParams();
  const isNew = userId === "new";

  const [id, setId] = useState("");
  const [name, setName] = useState("");
  const [loading, setLoading] = useState(!isNew);
  const [notFound, setNotFound] = useState(false);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState(null);

  const refresh = useCallback(() => {
    if (isNew) return;
    setLoading(true);
    setError(null);
    return getUser(userId)
      .then((u) => {
        setId(u.id);
        setName(u.name);
      })
      .catch((err) => {
        if (err.response?.status === 404) setNotFound(true);
        else setError(err.response?.data?.detail ?? err.message);
      })
      .finally(() => setLoading(false));
  }, [isNew, userId]);

  useEffect(() => {
    refresh();
  }, [refresh]);

  function handleSave() {
    setSaving(true);
    setError(null);
    const request = isNew ? createUser({ id, name }) : updateUser(userId, { name });
    request
      .then(() => navigate(isNew ? `/users/${id}` : "/users"))
      .catch((err) => setError(err.response?.data?.detail ?? err.message))
      .finally(() => setSaving(false));
  }

  function handleDelete() {
    setError(null);
    deleteUser(userId)
      .then(() => navigate("/users"))
      .catch((err) => setError(err.response?.data?.detail ?? err.message));
  }

  if (loading) {
    return (
      <div>
        <ListPageHeader breadcrumb="Operator Admin · Administration" title="User Management" action={<BackToListLink to="/users" />} />
        <p>Loading…</p>
      </div>
    );
  }

  if (notFound) {
    return (
      <div>
        <ListPageHeader breadcrumb="Operator Admin · Administration" title="User Management" action={<BackToListLink to="/users" />} />
        <p>User not found.</p>
      </div>
    );
  }

  return (
    <div>
      <ListPageHeader breadcrumb="Operator Admin · Administration" title="User Management" action={<BackToListLink to="/users" />} />
      {error && <p style={{ color: "var(--color-error)" }}>{error}</p>}
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
            {isNew ? "+" : initialsOf(name)}
          </div>
          <div style={{ fontSize: 18, fontWeight: 600 }}>{isNew ? "New user" : name}</div>
        </div>
        <div style={grid2}>
          {isNew ? (
            <div>
              <div style={label}>ID (Keycloak subject)</div>
              <input type="text" value={id} onChange={(e) => setId(e.target.value)} placeholder="e.g. a keycloak user's sub" />
            </div>
          ) : (
            <div>
              <div style={label}>ID (Keycloak subject)</div>
              <div style={{ ...readonly, fontFamily: "var(--font-mono, monospace)" }}>{id}</div>
            </div>
          )}
          <div>
            <div style={label}>Name</div>
            <input type="text" value={name} onChange={(e) => setName(e.target.value)} />
          </div>
        </div>
        <div
          style={{
            display: "flex",
            justifyContent: isNew ? "flex-end" : "space-between",
            alignItems: "center",
            marginTop: 32,
            paddingTop: 20,
            borderTop: "1px solid var(--color-border)",
          }}
        >
          {!isNew && (
            <button type="button" onClick={handleDelete} style={deleteButton}>
              <Trash2 size={18} strokeWidth={1.5} />
              Delete user
            </button>
          )}
          <div style={{ display: "flex", gap: 10 }}>
            <button type="button" onClick={() => navigate("/users")} style={secondaryButton}>
              Cancel
            </button>
            <button
              type="button"
              onClick={handleSave}
              disabled={saving || !name.trim() || (isNew && !id.trim())}
              style={primaryButton}
            >
              {saving ? "Saving…" : "Save changes"}
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}
