import { createContext, useContext, useMemo, useState } from "react";
import { getSelectedOrgId, setSelectedOrgId } from "./orgStore";

const OrgContext = createContext(null);

// Which org the signed-in user is currently acting within -- selected once after login (see
// SelectOrgPage/RequireOrg) and attached as the X-Org-Id header to every backend call from then
// on (api/client.js), so CasbinAuthorizationManager's "master-less" org resolution on the backend
// has something to read for endpoints whose path itself carries no org id.
export function OrgProvider({ children }) {
  const [selectedOrgId, setSelectedOrgIdState] = useState(() => getSelectedOrgId());

  const value = useMemo(
    () => ({
      selectedOrgId,
      selectOrg: (orgId) => {
        setSelectedOrgId(orgId);
        setSelectedOrgIdState(orgId);
      },
      clearOrg: () => {
        setSelectedOrgId(null);
        setSelectedOrgIdState(null);
      },
    }),
    [selectedOrgId],
  );

  return <OrgContext.Provider value={value}>{children}</OrgContext.Provider>;
}

export function useOrg() {
  const ctx = useContext(OrgContext);
  if (!ctx) throw new Error("useOrg must be used within an OrgProvider");
  return ctx;
}
