import { Navigate, useLocation } from "react-router-dom";
import { useOrg } from "./OrgContext";

// Wrap routes that need an org context with this (nested inside RequireAuth, which already
// guarantees a logged-in user by the time this runs). No org selected yet -> straight to
// /select-org, same "redirect rather than show a blocked page" approach as RequireAuth.
export function RequireOrg({ children }) {
  const { selectedOrgId } = useOrg();
  const location = useLocation();

  if (!selectedOrgId) {
    return <Navigate to="/select-org" replace state={{ from: location }} />;
  }

  return children;
}
