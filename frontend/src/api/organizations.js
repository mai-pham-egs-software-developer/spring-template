import apiClient from "./client";

// Backend's OrganizationController/RoleController/OrganizationUserController
// (com.my.craft.security.web.organization).
export function listOrganizations() {
  return apiClient.get("/bizz/organizations").then((res) => res.data);
}

export function getOrganization(id) {
  return apiClient.get(`/bizz/organizations/${id}`).then((res) => res.data);
}

export function createOrganization({ name }) {
  return apiClient.post("/bizz/organizations", { name }).then((res) => res.data);
}

export function updateOrganization(id, { name }) {
  return apiClient.put(`/bizz/organizations/${id}`, { name }).then((res) => res.data);
}

export function deleteOrganization(id) {
  return apiClient.delete(`/bizz/organizations/${id}`);
}

// Roles -- always scoped to one organization.
export function listRoles(organizationId) {
  return apiClient.get(`/bizz/organizations/${organizationId}/roles`).then((res) => res.data);
}

export function createRole(organizationId, { name }) {
  return apiClient.post(`/bizz/organizations/${organizationId}/roles`, { name }).then((res) => res.data);
}

export function updateRole(organizationId, roleId, { name }) {
  return apiClient.put(`/bizz/organizations/${organizationId}/roles/${roleId}`, { name }).then((res) => res.data);
}

export function deleteRole(organizationId, roleId) {
  return apiClient.delete(`/bizz/organizations/${organizationId}/roles/${roleId}`);
}

// Members -- a (organization, user) pair holds at most one role through this API (see
// backend's OrganizationMembershipService); add/update both just replace it.
export function listMembers(organizationId) {
  return apiClient.get(`/bizz/organizations/${organizationId}/users`).then((res) => res.data);
}

export function addMember(organizationId, { userId, roleId }) {
  return apiClient.post(`/bizz/organizations/${organizationId}/users`, { userId, roleId }).then((res) => res.data);
}

export function updateMember(organizationId, userId, { roleId }) {
  return apiClient.put(`/bizz/organizations/${organizationId}/users/${userId}`, { roleId }).then((res) => res.data);
}

export function removeMember(organizationId, userId) {
  return apiClient.delete(`/bizz/organizations/${organizationId}/users/${userId}`);
}

// Reverse lookup -- every org a given user belongs to.
export function listOrganizationsOfUser(userId) {
  return apiClient.get(`/bizz/users/${userId}/organizations`).then((res) => res.data);
}
