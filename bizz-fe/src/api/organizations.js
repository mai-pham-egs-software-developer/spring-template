import apiClient from "./client";

// Backend's OrganizationController.listMine (com.my.craft.security.web.organization), served
// under Constants.BASE_PATH = "/biz". userId comes from the caller's own token server-side, so
// this needs no argument and is exempt from the org/role check (casbin.rbac-exempt-paths).
export function listMyOrganizations() {
  return apiClient.get("/biz/organizations/me").then((res) => res.data);
}
