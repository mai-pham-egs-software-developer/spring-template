// Plain (non-React) singleton for the currently selected org -- mirrors ../auth/keycloak.js's
// role as a module-level instance that both React (OrgContext) and non-React code (api/client.js's
// axios interceptor, which can't call a hook) need to read. sessionStorage, not localStorage, so a
// closed tab/browser always re-prompts for an org on the next login rather than silently reusing
// a stale selection.
const STORAGE_KEY = "bizz.selectedOrgId";

export function getSelectedOrgId() {
  try {
    return sessionStorage.getItem(STORAGE_KEY);
  } catch {
    return null;
  }
}

export function setSelectedOrgId(orgId) {
  try {
    if (orgId == null) {
      sessionStorage.removeItem(STORAGE_KEY);
    } else {
      sessionStorage.setItem(STORAGE_KEY, String(orgId));
    }
  } catch {
    // Storage unavailable (private mode, disabled) -- org selection just won't persist
    // across a reload; OrgContext's in-memory state still works for the current page load.
  }
}
