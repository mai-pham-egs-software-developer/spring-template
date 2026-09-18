import { useCallback, useEffect, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { Trash2 } from "lucide-react";
import { createUser, deleteUser, getUser, updateUser } from "../../api/users";
import { listOrganizationsOfUser } from "../../api/organizations";
import { BackToListLink, ListPageHeader } from "../../layout/CrmPageHeader";
import { card, deleteButton, grid2, initialsOf, label, primaryButton, readonly, secondaryButton, tableCard, td, th } from "../../layout/crmStyles";
import { UserStatusPill } from "./UserStatusPill";

export function UserDetailPage() {
  const navigate = useNavigate();
  const { userId } = useParams();
  const isNew = userId === "new";

  const [id, setId] = useState("");
  const [username, setUsername] = useState("");
  const [email, setEmail] = useState("");
  const [name, setName] = useState("");
  const [status, setStatus] = useState(null);
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
        setUsername(u.username);
        setEmail(u.email);
        setName(u.name);
        setStatus(u.status);
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
    if (isNew) {
      // create() only writes the local row (status PENDING) and returns -- a background worker
      // syncs it to Keycloak asynchronously, see backend/docs/user-outbox.md. Land on the detail
      // view so the operator can see (and later refresh to watch) that status.
      createUser({ username, email, name })
        .then((u) => navigate(`/users/${u.id}`))
        .catch((err) => setError(err.response?.data?.detail ?? err.message))
        .finally(() => setSaving(false));
      return;
    }
    updateUser(userId, { name })
      .then(() => navigate("/users"))
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
      <div style={{ ...card, marginBottom: isNew ? 0 : 28 }}>
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
          {!isNew && status && <UserStatusPill status={status} />}
        </div>
        {!isNew && status === "PENDING" && (
          <p style={{ color: "var(--color-fg-2)", marginTop: -14, marginBottom: 20 }}>
            Syncing to Keycloak in the background -- refresh this page in a moment to see it become Active.
          </p>
        )}
        {!isNew && status === "CONFLICT" && (
          <p style={{ color: "var(--color-error)", marginTop: -14, marginBottom: 20 }}>
            Keycloak already has an account for this username that doesn't confidently match this person -- needs
            manual review.
          </p>
        )}
        {!isNew && status === "FAILED" && (
          <p style={{ color: "var(--color-error)", marginTop: -14, marginBottom: 20 }}>
            Keycloak permanently rejected creating this account -- needs manual review.
          </p>
        )}
        <div style={grid2}>
          {isNew ? (
            <>
              <div>
                <div style={label}>Username</div>
                <input type="text" value={username} onChange={(e) => setUsername(e.target.value)} placeholder="e.g. jane.doe" />
              </div>
              <div>
                <div style={label}>Email (optional)</div>
                <input type="email" value={email} onChange={(e) => setEmail(e.target.value)} placeholder="jane.doe@example.com" />
              </div>
              <div>
                <div style={label}>Name</div>
                <input type="text" value={name} onChange={(e) => setName(e.target.value)} />
              </div>
            </>
          ) : (
            <>
              <div>
                <div style={label}>Username</div>
                <div style={readonly}>{username}</div>
              </div>
              <div>
                <div style={label}>Email</div>
                <div style={readonly}>{email || "—"}</div>
              </div>
              <div>
                <div style={label}>Name</div>
                <input type="text" value={name} onChange={(e) => setName(e.target.value)} />
              </div>
              <div>
                <div style={label}>ID</div>
                <div style={{ ...readonly, fontFamily: "var(--font-mono, monospace)" }}>{id}</div>
              </div>
            </>
          )}
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
              disabled={saving || !name.trim() || (isNew && !username.trim())}
              style={primaryButton}
            >
              {saving ? "Saving…" : isNew ? "Create user" : "Save changes"}
            </button>
          </div>
        </div>
      </div>

      {!isNew && <OrganizationsOfUserSection userId={userId} />}
    </div>
  );
}

function OrganizationsOfUserSection({ userId }) {
  const [organizations, setOrganizations] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    setLoading(true);
    setError(null);
    listOrganizationsOfUser(userId)
      .then(setOrganizations)
      .catch((err) => setError(err.response?.data?.detail ?? err.message))
      .finally(() => setLoading(false));
  }, [userId]);

  return (
    <div>
      <div style={{ fontSize: 15, fontWeight: 600, marginBottom: 12 }}>Organizations</div>
      {error && <p style={{ color: "var(--color-error)" }}>{error}</p>}
      <div style={{ ...tableCard, maxWidth: 720 }}>
        <table style={{ width: "100%" }}>
          <thead>
            <tr style={{ background: "var(--color-mist)" }}>
              <th style={th}>Name</th>
              <th style={th}>ID</th>
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
            {!loading && organizations.length === 0 && (
              <tr>
                <td style={td} colSpan={2}>
                  Not a member of any organization.
                </td>
              </tr>
            )}
            {!loading &&
              organizations.map((org) => (
                <tr key={org.id}>
                  <td style={{ ...td, fontWeight: 500 }}>{org.name}</td>
                  <td style={{ ...td, color: "var(--color-fg-2)" }}>{org.id}</td>
                </tr>
              ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}
