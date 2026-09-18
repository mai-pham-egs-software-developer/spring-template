import { Fragment, useCallback, useEffect, useState } from "react";
import { ChevronDown, ChevronRight, RefreshCw } from "lucide-react";
import { listEntityHistory, listRecentAuditLog } from "../../api/auditLog";
import { ListPageHeader } from "../../layout/CrmPageHeader";
import { pill, primaryButton, tableCard, td, th } from "../../layout/crmStyles";

const SNAPSHOT_TYPE_STYLES = {
  INITIAL: pill("var(--color-purple-100)", "var(--color-purple-700)"),
  UPDATE: pill("var(--color-mist)", "var(--color-fg-2)"),
  TERMINAL: pill("var(--color-error-bg)", "var(--color-error)"),
};

// globalId looks like "com.my.craft.security.domain.User/e5a20aac-..." -- split into the entity's
// fully-qualified class name (needed to call listEntityHistory), a short display name (its last
// segment), and the local id.
function splitGlobalId(globalId) {
  const slash = globalId.lastIndexOf("/");
  if (slash === -1) {
    return { className: globalId, type: globalId, id: "" };
  }
  const className = globalId.slice(0, slash);
  const id = globalId.slice(slash + 1);
  const dot = className.lastIndexOf(".");
  return { className, type: dot === -1 ? className : className.slice(dot + 1), id };
}

export function AuditLogPage() {
  const [entries, setEntries] = useState([]);
  const [limit, setLimit] = useState(100);
  const [action, setAction] = useState("");
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [expandedIndex, setExpandedIndex] = useState(null);

  const refresh = useCallback(() => {
    setLoading(true);
    setError(null);
    return listRecentAuditLog(limit, action.trim())
      .then(setEntries)
      .catch((err) => setError(err.response?.data?.detail ?? err.message))
      .finally(() => setLoading(false));
  }, [limit, action]);

  useEffect(() => {
    refresh();
  }, [refresh]);

  return (
    <div>
      <ListPageHeader
        breadcrumb="Operator Admin · Administration"
        title="Audit Log"
        action={
          <button
            type="button"
            onClick={refresh}
            disabled={loading}
            style={{ ...primaryButton, display: "flex", alignItems: "center", gap: 6 }}
          >
            <RefreshCw size={16} strokeWidth={1.5} />
            Refresh
          </button>
        }
      />

      <div style={{ display: "flex", alignItems: "center", gap: 8, marginBottom: 16, flexWrap: "wrap" }}>
        <label style={{ fontSize: 13, color: "var(--color-fg-2)" }} htmlFor="audit-log-limit">
          Show last
        </label>
        <input
          id="audit-log-limit"
          type="number"
          min={1}
          max={1000}
          value={limit}
          onChange={(e) => setLimit(Number(e.target.value) || 1)}
          style={{ width: 80 }}
        />
        <span style={{ fontSize: 13, color: "var(--color-fg-2)" }}>entries</span>

        <label style={{ fontSize: 13, color: "var(--color-fg-2)", marginLeft: 12 }} htmlFor="audit-log-action">
          Marked action
        </label>
        <input
          id="audit-log-action"
          type="text"
          value={action}
          onChange={(e) => setAction(e.target.value)}
          placeholder="e.g. USER_CREATED (leave empty for all)"
          style={{ width: 260 }}
        />
      </div>

      {error && <p style={{ color: "var(--color-error)" }}>{error}</p>}

      <div style={{ ...tableCard, overflowX: "auto" }}>
        <table style={{ minWidth: 720, width: "100%" }}>
          <thead>
            <tr style={{ background: "var(--color-mist)" }}>
              <th style={{ ...th, width: 32 }}></th>
              <th style={th}>Entity</th>
              <th style={th}>Marked action</th>
              <th style={th}>Type</th>
              <th style={th}>Author</th>
              <th style={th}>Committed at</th>
            </tr>
          </thead>
          <tbody>
            {loading && (
              <tr>
                <td style={td} colSpan={6}>
                  Loading…
                </td>
              </tr>
            )}
            {!loading && entries.length === 0 && (
              <tr>
                <td style={td} colSpan={6}>
                  No audit history yet.
                </td>
              </tr>
            )}
            {!loading &&
              entries.map((entry, index) => {
                const { className, type, id } = splitGlobalId(entry.globalId);
                const expanded = expandedIndex === index;
                return (
                  <Fragment key={`${entry.globalId}-${entry.commitId}`}>
                    <tr onClick={() => setExpandedIndex(expanded ? null : index)} style={{ cursor: "pointer" }}>
                      <td style={{ ...td, textAlign: "center", color: "var(--color-fg-3)" }}>
                        {expanded ? <ChevronDown size={16} strokeWidth={1.5} /> : <ChevronRight size={16} strokeWidth={1.5} />}
                      </td>
                      <td style={td}>
                        <div style={{ fontWeight: 500 }}>{type}</div>
                        <div style={{ color: "var(--color-fg-2)", fontFamily: "var(--font-mono, monospace)", fontSize: 12 }}>{id}</div>
                      </td>
                      <td style={{ ...td, fontWeight: 500 }}>{entry.markedAction ?? "—"}</td>
                      <td style={td}>
                        <span style={SNAPSHOT_TYPE_STYLES[entry.snapshotType] ?? SNAPSHOT_TYPE_STYLES.UPDATE}>{entry.snapshotType}</span>
                      </td>
                      <td style={{ ...td, color: "var(--color-fg-2)" }}>{entry.author ?? "—"}</td>
                      <td style={{ ...td, color: "var(--color-fg-2)" }}>{entry.committedAt?.replace("T", " ") ?? "—"}</td>
                    </tr>
                    {expanded && (
                      <tr>
                        <td style={{ ...td, background: "var(--color-mist)" }}></td>
                        <td style={{ ...td, background: "var(--color-mist)" }} colSpan={5}>
                          <AuditEntryDetail entry={entry} entityClassName={className} entityId={id} />
                        </td>
                      </tr>
                    )}
                  </Fragment>
                );
              })}
          </tbody>
        </table>
      </div>
    </div>
  );
}

// listRecentAuditLog never returns `before` (it's a cross-entity feed, not one entity's timeline
// -- see backend/docs/audit-log.md), so an expanded row fetches that ONE entity's full history on
// demand (only when actually expanded, not for the whole list up front) and pulls the matching
// commit's `before` out of it -- listEntityHistory's results already have `before` computed by
// the backend, so this just needs to find the right commitId, not recompute anything.
function AuditEntryDetail({ entry, entityClassName, entityId }) {
  const [before, setBefore] = useState(entry.before ?? null);
  const [loading, setLoading] = useState(entry.before == null);
  const [error, setError] = useState(null);

  useEffect(() => {
    if (entry.before != null) {
      return; // already have it (a history view would populate this directly)
    }
    setLoading(true);
    setError(null);
    listEntityHistory(entityClassName, entityId)
      .then((history) => {
        const match = history.find((h) => h.commitId === entry.commitId);
        setBefore(match?.before ?? null);
      })
      .catch((err) => setError(err.response?.data?.detail ?? err.message))
      .finally(() => setLoading(false));
    // entry.commitId uniquely identifies which snapshot to look up; the rest of `entry` doesn't
    // need to be a dependency since it doesn't change for a given commitId.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [entityClassName, entityId, entry.commitId]);

  if (loading) {
    return <span style={{ color: "var(--color-fg-2)" }}>Loading diff…</span>;
  }
  if (error) {
    return <span style={{ color: "var(--color-error)" }}>{error}</span>;
  }
  return <AuditStateTable before={before} after={entry.after} />;
}

function AuditStateTable({ before, after }) {
  const properties = Object.keys(after ?? {}).sort();
  if (properties.length === 0) {
    return <span style={{ color: "var(--color-fg-2)" }}>No properties recorded.</span>;
  }
  if (!before) {
    return (
      <table style={{ width: "100%" }}>
        <tbody>
          {properties.map((property) => (
            <tr key={property}>
              <td style={{ padding: "4px 8px 4px 0", fontWeight: 500, verticalAlign: "top", whiteSpace: "nowrap" }}>{property}</td>
              <td style={{ padding: "4px 0", fontFamily: "var(--font-mono, monospace)", wordBreak: "break-word" }}>
                {formatValue(after[property])}
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    );
  }
  return (
    <table style={{ width: "100%" }}>
      <thead>
        <tr>
          <th style={{ textAlign: "left", padding: "0 8px 4px 0", fontSize: 12, color: "var(--color-fg-3)" }}>Property</th>
          <th style={{ textAlign: "left", padding: "0 8px 4px 0", fontSize: 12, color: "var(--color-fg-3)" }}>Before</th>
          <th style={{ textAlign: "left", padding: "0 0 4px 0", fontSize: 12, color: "var(--color-fg-3)" }}>After</th>
        </tr>
      </thead>
      <tbody>
        {properties.map((property) => {
          const changed = formatValue(before[property]) !== formatValue(after[property]);
          return (
            <tr key={property}>
              <td style={{ padding: "4px 8px 4px 0", fontWeight: 500, verticalAlign: "top", whiteSpace: "nowrap" }}>{property}</td>
              <td
                style={{
                  padding: "4px 8px 4px 0",
                  fontFamily: "var(--font-mono, monospace)",
                  wordBreak: "break-word",
                  color: changed ? "var(--color-error)" : "var(--color-fg-2)",
                  textDecoration: changed ? "line-through" : "none",
                }}
              >
                {formatValue(before[property])}
              </td>
              <td
                style={{
                  padding: "4px 0",
                  fontFamily: "var(--font-mono, monospace)",
                  wordBreak: "break-word",
                  fontWeight: changed ? 600 : 400,
                }}
              >
                {formatValue(after[property])}
              </td>
            </tr>
          );
        })}
      </tbody>
    </table>
  );
}

function formatValue(value) {
  if (value === null || value === undefined) {
    return "—";
  }
  if (typeof value === "object") {
    return JSON.stringify(value);
  }
  return String(value);
}
