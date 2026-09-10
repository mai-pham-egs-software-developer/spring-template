import { useParams } from "react-router-dom";
import { AUDIT } from "../../data/crmMockData";
import { BackToListLink, ListPageHeader } from "../../layout/CrmPageHeader";
import { card, label, readonly, severityPillStyle } from "../../layout/crmStyles";

export function AuditDetailPage() {
  const { auditId } = useParams();
  const entry = AUDIT.find((a) => a.id === auditId);

  if (!entry) {
    return (
      <div>
        <ListPageHeader breadcrumb="CRM · Administration" title="Audit Log" action={<BackToListLink to="/audit-log" />} />
        <p>Audit entry not found.</p>
      </div>
    );
  }

  return (
    <div>
      <ListPageHeader breadcrumb="CRM · Administration" title="Audit Log" action={<BackToListLink to="/audit-log" />} />
      <div style={{ ...card, maxWidth: 680 }}>
        <div style={{ display: "flex", alignItems: "center", justifyContent: "space-between", marginBottom: 24 }}>
          <div>
            <div style={{ fontSize: 18, fontWeight: 600 }}>{entry.action}</div>
            <div style={{ color: "var(--color-fg-3)", fontSize: 13, marginTop: 2 }}>{entry.timestamp}</div>
          </div>
          <span style={severityPillStyle(entry.severity)}>{entry.severity}</span>
        </div>
        <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: 16, marginBottom: 24 }}>
          <div>
            <div style={label}>User</div>
            <div style={readonly}>{entry.user}</div>
          </div>
          <div>
            <div style={label}>IP address</div>
            <div style={readonly}>{entry.ip}</div>
          </div>
          <div style={{ gridColumn: "span 2" }}>
            <div style={label}>Entity / target</div>
            <div style={readonly}>{entry.entity}</div>
          </div>
        </div>
        <div style={label}>Change detail</div>
        <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: 12, marginTop: 8 }}>
          <div
            style={{
              border: "1px solid var(--color-border)",
              borderRadius: "var(--radius-sm)",
              padding: "12px 14px",
              background: "var(--color-error-bg)",
            }}
          >
            <div
              style={{
                fontSize: 11,
                fontWeight: 600,
                color: "var(--color-error)",
                textTransform: "uppercase",
                letterSpacing: "0.04em",
                marginBottom: 6,
              }}
            >
              Before
            </div>
            <div style={{ fontFamily: "var(--font-mono)", fontSize: 13, color: "var(--color-fg-1)" }}>{entry.before}</div>
          </div>
          <div
            style={{
              border: "1px solid var(--color-border)",
              borderRadius: "var(--radius-sm)",
              padding: "12px 14px",
              background: "var(--color-success-bg)",
            }}
          >
            <div
              style={{
                fontSize: 11,
                fontWeight: 600,
                color: "var(--color-success)",
                textTransform: "uppercase",
                letterSpacing: "0.04em",
                marginBottom: 6,
              }}
            >
              After
            </div>
            <div style={{ fontFamily: "var(--font-mono)", fontSize: 13, color: "var(--color-fg-1)" }}>{entry.after}</div>
          </div>
        </div>
      </div>
    </div>
  );
}
