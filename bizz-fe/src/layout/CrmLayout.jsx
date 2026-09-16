import { Outlet, useLocation, useNavigate } from "react-router-dom";
import { LogOut, Sparkles, Users } from "lucide-react";
import { useAuth } from "../auth/AuthContext";
import { useOrg } from "../org/OrgContext";
import { initialsOf, navActive, navBase } from "./crmStyles";

const NAV_ITEMS = [
  { to: "/profile", label: "My Profiles", icon: Users },
  { to: "/api-demo", label: "API Demo", icon: Sparkles },
];

export function CrmLayout() {
  const navigate = useNavigate();
  const location = useLocation();
  const { user, hasRole, logout } = useAuth();
  const { clearOrg } = useOrg();

  const displayName = user?.preferred_username ?? user?.email ?? "Unknown user";

  return (
    <div style={{ display: "flex", minHeight: "100vh", fontFamily: "var(--font-sans)" }}>
      <div
        style={{
          width: 240,
          flex: "none",
          background: "var(--color-ink-900)",
          display: "flex",
          flexDirection: "column",
          position: "sticky",
          top: 0,
          height: "100vh",
        }}
      >
        <div
          style={{
            display: "flex",
            alignItems: "center",
            gap: 10,
            padding: "24px 20px",
            borderBottom: "1px solid rgba(255,255,255,0.08)",
          }}
        >
          {/* assets/egs-mark-white.svg from the design export wasn't included in the
              download -- falling back to the repo's real Eastgate mark instead. */}
          <img src="/favicon.svg" alt="Eastgate" style={{ width: 28, height: 28, flex: "none" }} />
          <div style={{ color: "#fff", fontWeight: 600, fontSize: 15, letterSpacing: "-0.01em" }}>Operator Admin</div>
        </div>

        <div style={{ display: "flex", flexDirection: "column", gap: 2, padding: "16px 12px", flex: 1 }}>
          {NAV_ITEMS.map(({ to, label, icon: Icon }) => {
            const active = location.pathname.startsWith(to);
            return (
              <div key={to} onClick={() => navigate(to)} style={active ? navActive : navBase}>
                <Icon size={18} strokeWidth={1.5} style={{ flex: "none" }} />
                {label}
              </div>
            );
          })}
        </div>

        <div
          style={{
            display: "flex",
            alignItems: "center",
            gap: 10,
            padding: "18px 20px",
            borderTop: "1px solid rgba(255,255,255,0.08)",
          }}
        >
          <div
            style={{
              width: 32,
              height: 32,
              borderRadius: 999,
              background: "var(--color-purple)",
              color: "#fff",
              display: "flex",
              alignItems: "center",
              justifyContent: "center",
              fontSize: 12,
              fontWeight: 600,
              flex: "none",
            }}
          >
            {initialsOf(displayName)}
          </div>
          <div style={{ minWidth: 0, flex: 1 }}>
            <div
              style={{
                color: "#fff",
                fontSize: 13,
                fontWeight: 500,
                overflow: "hidden",
                textOverflow: "ellipsis",
                whiteSpace: "nowrap",
              }}
            >
              {displayName}
            </div>
            <div style={{ color: "var(--color-ink-300)", fontSize: 12 }}>
              {hasRole("admin") ? "Administrator" : "Member"}
            </div>
          </div>
          <button
            type="button"
            onClick={() => {
              clearOrg();
              logout();
            }}
            title="Log out"
            style={{
              background: "none",
              border: "none",
              color: "var(--color-ink-300)",
              cursor: "pointer",
              display: "flex",
              padding: 4,
              flex: "none",
            }}
          >
            <LogOut size={18} strokeWidth={1.5} />
          </button>
        </div>
      </div>

      <div style={{ flex: 1, display: "flex", flexDirection: "column", minWidth: 0 }}>
        <div style={{ flex: 1, padding: 32, overflow: "auto" }}>
          <Outlet />
        </div>
      </div>
    </div>
  );
}
