import apiClient from "./client";

// Backend's UserController (com.my.craft.security.web.operator) -- plain CRUD over the
// persisted User (id = Keycloak subject claim, assigned by the caller, not generated).
export function listUsers() {
  return apiClient.get("/operators/users").then((res) => res.data);
}

export function getUser(id) {
  return apiClient.get(`/operators/users/${id}`).then((res) => res.data);
}

export function createUser({ id, name }) {
  return apiClient.post("/operators/users", { id, name }).then((res) => res.data);
}

export function updateUser(id, { name }) {
  return apiClient.put(`/operators/users/${id}`, { name }).then((res) => res.data);
}

export function deleteUser(id) {
  return apiClient.delete(`/operators/users/${id}`);
}
