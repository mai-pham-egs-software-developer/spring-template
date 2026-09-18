import apiClient from "./client";

// Backend's UserController (com.my.craft.security.web.operator) -- plain CRUD over the
// persisted User. id is an app-generated local key, NOT the Keycloak subject claim -- create()
// writes the local row (status PENDING) and returns immediately (202 Accepted); a background
// worker syncs it to Keycloak asynchronously. See backend/docs/user-outbox.md.
export function listUsers() {
  return apiClient.get("/operators/users").then((res) => res.data);
}

export function getUser(id) {
  return apiClient.get(`/operators/users/${id}`).then((res) => res.data);
}

export function createUser({ username, email, name }) {
  return apiClient.post("/operators/users", { username, email, name }).then((res) => res.data);
}

export function updateUser(id, { name }) {
  return apiClient.put(`/operators/users/${id}`, { name }).then((res) => res.data);
}

export function deleteUser(id) {
  return apiClient.delete(`/operators/users/${id}`);
}
