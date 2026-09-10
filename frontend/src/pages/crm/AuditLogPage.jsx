import { useNavigate } from "react-router-dom";
import { ChevronRight } from "lucide-react";
import { AUDIT } from "../../data/crmMockData";
import { ListPageHeader } from "../../layout/CrmPageHeader";
import { row, severityPillStyle, tableCard, td, th } from "../../layout/crmStyles";

export function AuditLogPage() {
  const navigate = useNavigate();

  return (
    <div>
      <ListPageHeader breadcrumb="CRM · Administration" title="Audit Log" searchPlaceholder="Search audit log…" />

      <div style={{ ...tableCard, overflowX: "auto" }}>
        <table style={{ minWidth: 720 }}>
          <thead>
            <tr style={{ background: "var(--color-mist)" }}>
              <th style={th}>Timestamp</th>
              <th style={th}>User</th>
              <th style={th}>Action</th>
              <th style={th}>Entity</th>
              <th style={th}>Severity</th>
              <th style={th}></th>
            </tr>
          </thead>
          <tbody>
            {AUDIT.map((a) => (
              <tr key={a.id} onClick={() => navigate(`/audit-log/${a.id}`)} style={row}>
                <td style={{ ...td, color: "var(--color-fg-2)", whiteSpace: "nowrap" }}>{a.timestamp}</td>
                <td style={{ ...td, fontWeight: 500 }}>{a.user}</td>
                <td style={{ ...td, color: "var(--color-fg-2)" }}>{a.action}</td>
                <td style={{ ...td, color: "var(--color-fg-2)" }}>{a.entity}</td>
                <td style={td}>
                  <span style={severityPillStyle(a.severity)}>{a.severity}</span>
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
