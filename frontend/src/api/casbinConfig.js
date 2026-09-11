import apiClient from "./client";

// Backend's CasbinPolicyController /config -- the RBAC model definition (the sub/obj/act +
// matcher text), DB-backed (table casbin_model_config) so it survives a restart. See
// backend/docs/security.md.
export function getModelConfig() {
  return apiClient.get("/operators/casbin/config").then((res) => res.data);
}

export function updateModelConfig(content) {
  return apiClient.put("/operators/casbin/config", { content }).then((res) => res.data);
}
