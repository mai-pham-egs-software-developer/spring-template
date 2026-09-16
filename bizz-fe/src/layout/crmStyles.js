// Shared style-object constants for the CRM UI, mirroring the TH/TD/ROW/PRIMARY_BTN/
// pill() constants in frontend/template/CRM.dc.html -- kept as plain style objects
// (not components) since the original design itself doesn't componentize beyond this.

export const navBase = {
  display: "flex",
  alignItems: "center",
  gap: 12,
  padding: "10px 12px",
  borderRadius: 6,
  color: "var(--color-ink-300)",
  fontSize: 14,
  fontWeight: 500,
  cursor: "pointer",
};

export const navActive = {
  ...navBase,
  color: "#fff",
  background: "rgba(255,255,255,0.1)",
};

export const th = {
  textAlign: "left",
  padding: "13px 20px",
  fontSize: 12,
  fontWeight: 600,
  color: "var(--color-fg-3)",
  textTransform: "uppercase",
  letterSpacing: "0.04em",
  borderBottom: "1px solid var(--color-border)",
};

export const td = {
  padding: "14px 20px",
  fontSize: 14,
  borderBottom: "1px solid var(--color-border)",
  whiteSpace: "nowrap",
};

export const row = {
  cursor: "pointer",
};

export const label = {
  fontSize: 12,
  fontWeight: 600,
  color: "var(--color-fg-3)",
  marginBottom: 6,
};

export const readonly = {
  fontSize: 14,
  color: "var(--color-fg-2)",
  padding: "9px 0",
};

export const primaryButton = {
  background: "var(--color-purple)",
  color: "#fff",
  border: "none",
  borderRadius: "var(--radius-sm)",
  padding: "10px 18px",
  fontSize: 14,
  fontWeight: 500,
  cursor: "pointer",
};

export const secondaryButton = {
  display: "flex",
  alignItems: "center",
  gap: 6,
  background: "none",
  color: "var(--color-fg-2)",
  border: "1px solid var(--color-border-strong)",
  borderRadius: "var(--radius-sm)",
  padding: "10px 16px",
  fontSize: 14,
  fontWeight: 500,
  cursor: "pointer",
  textDecoration: "none",
};

export const deleteButton = {
  display: "flex",
  alignItems: "center",
  gap: 6,
  background: "none",
  color: "var(--color-error)",
  border: "none",
  fontSize: 14,
  fontWeight: 500,
  cursor: "pointer",
  padding: 0,
};

export const card = {
  maxWidth: 640,
  background: "var(--color-white)",
  borderRadius: "var(--radius-md)",
  boxShadow: "var(--shadow-card)",
  padding: 32,
};

export const grid2 = {
  display: "grid",
  gridTemplateColumns: "1fr 1fr",
  gap: "20px 16px",
};

export const tableCard = {
  background: "var(--color-white)",
  borderRadius: "var(--radius-md)",
  boxShadow: "var(--shadow-card)",
  overflow: "hidden",
};

export function pill(bg, fg) {
  return {
    display: "inline-flex",
    alignItems: "center",
    padding: "4px 10px",
    borderRadius: 999,
    fontSize: 12,
    fontWeight: 600,
    background: bg,
    color: fg,
  };
}

export function severityPillStyle(severity) {
  if (severity === "Error") return pill("var(--color-error-bg)", "var(--color-error)");
  if (severity === "Warning") return pill("var(--color-warning-bg)", "var(--color-warning)");
  return pill("var(--color-mist)", "var(--color-fg-2)");
}

export function initialsOf(name) {
  return (name || "")
    .split(" ")
    .filter(Boolean)
    .map((w) => w[0])
    .slice(0, 2)
    .join("")
    .toUpperCase();
}
