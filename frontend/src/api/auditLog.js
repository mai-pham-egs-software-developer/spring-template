import apiClient from "./client";

// Backend's AuditLogController -- self-hosted in modules/audit-log (com.my.craft.auditlog.web),
// not modules/security, to keep the dependency one-way (security depends on audit-log for
// AuditActionRecorder, not the other way around) -- see backend/docs/audit-log.md. Each entry's
// `state` is the FULL property snapshot as of that commit, not a diff against the previous one.
// Only explicitly marked actions show up here (AuditActionRecorder.record/recordDeletion calls in
// security's services) -- there's no blanket "every save" auto-audit anymore.
export function listRecentAuditLog(limit = 100, action = null) {
  return apiClient.get("/operators/audit-log", { params: { limit, action: action || undefined } }).then((res) => res.data);
}

// entityType is the entity's fully-qualified Java class name -- the controller resolves it by
// reflection (Class.forName) since audit-log has no compile-time dependency on security's domain
// classes. Unlike listRecentAuditLog, every entry here has `before` populated (see
// backend/docs/audit-log.md) -- it's a single entity's own timeline, not a cross-entity feed.
export function listEntityHistory(entityType, entityId) {
  return apiClient.get("/operators/audit-log/history", { params: { entityType, entityId } }).then((res) => res.data);
}

export function listUserAuditLog(userId) {
  return listEntityHistory("com.my.craft.security.domain.User", userId);
}

export function listOrganizationAuditLog(organizationId) {
  return listEntityHistory("com.my.craft.security.domain.Organization", organizationId);
}

export function listRoleAuditLog(roleId) {
  return listEntityHistory("com.my.craft.security.domain.Role", roleId);
}
