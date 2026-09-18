import apiClient from "./client";

// Plain CRUD -- backend's web.operator.OrganizationController.
export function listOrganizations() {
  return apiClient.get("/operators/organizations").then((res) => res.data);
}

export function getOrganization(id) {
  return apiClient.get(`/operators/organizations/${id}`).then((res) => res.data);
}

export function createOrganization({ name }) {
  return apiClient.post("/operators/organizations", { name }).then((res) => res.data);
}

export function updateOrganization(id, { name }) {
  return apiClient.put(`/operators/organizations/${id}`, { name }).then((res) => res.data);
}

export function deleteOrganization(id) {
  return apiClient.delete(`/operators/organizations/${id}`);
}

// Roles -- backend's web.operator.OrganizationController, always scoped to one organization.
export function listRoles(organizationId) {
  return apiClient.get(`/operators/organizations/${organizationId}/roles`).then((res) => res.data);
}

export function createRole(organizationId, { name }) {
  return apiClient.post(`/operators/organizations/${organizationId}/roles`, { name }).then((res) => res.data);
}

export function updateRole(organizationId, roleId, { name }) {
  return apiClient.put(`/operators/organizations/${organizationId}/roles/${roleId}`, { name }).then((res) => res.data);
}

export function deleteRole(organizationId, roleId) {
  return apiClient.delete(`/operators/organizations/${organizationId}/roles/${roleId}`);
}

// Members -- a (organization, user) pair holds at most one role through this API (see
// backend's OrganizationMembershipService); add/update both just replace it.
export function listMembers(organizationId) {
  return apiClient.get(`/operators/organizations/${organizationId}/users`).then((res) => res.data);
}

export function addMember(organizationId, { userId, roleId }) {
  return apiClient.post(`/operators/organizations/${organizationId}/users`, { userId, roleId }).then((res) => res.data);
}

export function updateMember(organizationId, userId, { roleId }) {
  return apiClient.put(`/operators/organizations/${organizationId}/users/${userId}`, { roleId }).then((res) => res.data);
}

export function removeMember(organizationId, userId) {
  return apiClient.delete(`/operators/organizations/${organizationId}/users/${userId}`);
}

// Reverse lookup -- every org a given user belongs to -- backend's web.operator.UserController.
export function listOrganizationsOfUser(userId) {
  return apiClient.get(`/operators/users/${userId}/organizations`).then((res) => res.data);
}
