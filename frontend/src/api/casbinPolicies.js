import apiClient from "./client";

// Backend's CasbinPolicyController /policies is dynamic by ptype: a ptype starting with "g" is a
// role/grouping assertion, anything else (default "p") a plain policy assertion -- see
// backend/docs/security.md. `params` is a plain string array whose length/meaning depends on the
// model (e.g. p: [role, pathPattern, method], g: [username, role]).
export function listPolicies(ptype = "p") {
  return apiClient.get("/operators/casbin/policies", { params: { ptype } }).then((res) => res.data);
}

export function addPolicy(ptype, params) {
  return apiClient.post("/operators/casbin/policies", { ptype, params });
}

export function updatePolicy(ptype, oldParams, newParams) {
  return apiClient.put("/operators/casbin/policies", { ptype, oldParams, newParams });
}

export function removePolicy(ptype, params) {
  return apiClient.delete("/operators/casbin/policies", { data: { ptype, params } });
}

// Only needed after a change made outside this controller (e.g. writing to casbin_rule
// directly) -- calls through the endpoints above are already live without it.
export function reloadPolicies() {
  return apiClient.post("/operators/casbin/reload");
}
