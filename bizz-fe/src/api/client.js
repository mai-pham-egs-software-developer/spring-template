import axios from "axios";
import keycloak from "../auth/keycloak";
import { getSelectedOrgId } from "../org/orgStore";

const apiClient = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL,
});

apiClient.interceptors.request.use(async (config) => {
  if (keycloak.authenticated) {
    try {
      await keycloak.updateToken(30);
    } catch {
      keycloak.login();
      throw new axios.Cancel("Session expired, redirecting to login");
    }
    config.headers.Authorization = `Bearer ${keycloak.token}`;
  }

  // Lets the backend's CasbinAuthorizationManager resolve orgId for a "master-less" endpoint
  // (one whose path carries no org id of its own) once an org has been selected -- see
  // org/OrgContext.jsx / pages/SelectOrgPage.jsx.
  const orgId = getSelectedOrgId();
  if (orgId) {
    config.headers["X-Org-Id"] = orgId;
  }

  return config;
});

export default apiClient;
