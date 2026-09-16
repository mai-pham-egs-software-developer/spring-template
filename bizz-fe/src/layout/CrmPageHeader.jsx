import { Link } from "react-router-dom";
import { ArrowLeft, Search } from "lucide-react";
import { secondaryButton } from "./crmStyles";

// Reused across every CRM list/detail page for the breadcrumb + title + optional
// search box + right-side action (create/upload button on list pages, "Back to
// list" on detail pages) -- CRM.dc.html renders this as a single persistent top
// bar; here each page owns it inline instead (see plan: simpler than threading
// title/action state up into the shared layout).
export function ListPageHeader({ breadcrumb, title, searchPlaceholder, action }) {
  return (
    <div style={{ display: "flex", alignItems: "center", justifyContent: "space-between", marginBottom: 24 }}>
      <div>
        <div style={{ fontSize: 12, color: "var(--color-fg-3)", marginBottom: 2 }}>{breadcrumb}</div>
        <h1 style={{ fontSize: 24, fontWeight: 600, lineHeight: 1.2, margin: 0 }}>{title}</h1>
      </div>
      {(searchPlaceholder || action) && (
        <div style={{ display: "flex", alignItems: "center", gap: 12 }}>
          {searchPlaceholder && (
            <div style={{ position: "relative" }}>
              <Search
                size={18}
                strokeWidth={1.5}
                style={{ position: "absolute", left: 12, top: "50%", transform: "translateY(-50%)", color: "var(--color-fg-3)" }}
              />
              <input type="text" placeholder={searchPlaceholder} style={{ width: 240, paddingLeft: 38 }} />
            </div>
          )}
          {action}
        </div>
      )}
    </div>
  );
}

export function BackToListLink({ to }) {
  return (
    <Link to={to} style={secondaryButton}>
      <ArrowLeft size={18} strokeWidth={1.5} />
      Back to list
    </Link>
  );
}
