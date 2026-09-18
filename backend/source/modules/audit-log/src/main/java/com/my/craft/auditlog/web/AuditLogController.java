package com.my.craft.auditlog.web;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.lang.Nullable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.my.craft.auditlog.dto.AuditLogEntryResponse;
import com.my.craft.auditlog.service.AuditQueryService;

/**
 * Read-only access to this module's JaVers-backed change history -- see
 * backend/docs/audit-log.md. Self-hosted here (not in {@code modules/security}, which merely
 * depends on this module for {@code AuditActionRecorder}) so the dependency stays one-way: this
 * class deliberately doesn't reference {@code User}/{@code Organization}/{@code Role} or {@code
 * @RequiresPermission} by name, since doing either would need a dependency back on {@code
 * security} -- and {@code security} already depends on this module, so that would be a cycle.
 *
 * <p>{@code entityType} is therefore a plain, caller-supplied fully-qualified class name (e.g.
 * {@code "com.my.craft.security.domain.User"}), resolved by reflection ({@link Class#forName}).
 * That's safe: JaVers only ever returns data for a class some {@code AuditActionRecorder.record}/
 * {@code recordDeletion} call has actually committed a snapshot for, so being able to *name* an
 * arbitrary class here doesn't expose anything beyond what's already been marked for audit.
 *
 * <p>No {@code @RequiresPermission} here (that annotation lives in {@code security}, unreachable
 * without the same cyclic dependency) -- {@code CasbinAuthorizationManager} still enforces
 * default-deny on every path under {@code /operators/**} even without it, falling back to
 * {@code objectType = request path, action = HTTP method} (see its javadoc and {@code
 * docs/security.md}); the same fallback already covers {@code CasbinPolicyController}/{@code
 * MeController}. The master-org bypass added there also still applies, since it only checks the
 * path prefix.
 */
@RestController
@RequestMapping("/operators/audit-log")
public class AuditLogController {

    private final AuditQueryService auditQueryService;

    public AuditLogController(AuditQueryService auditQueryService) {
        this.auditQueryService = auditQueryService;
    }

    /** Plain {@code ?limit=} for everything recorded; add {@code &action=<name>} to scope this to
     * one marked action (see {@code AuditActionRecorder}) -- e.g. {@code ?action=USER_CREATED}. */
    @GetMapping
    public List<AuditLogEntryResponse> recent(@RequestParam(defaultValue = "100") int limit, @RequestParam(required = false) @Nullable String action) {
        return action == null ? auditQueryService.findRecent(limit) : auditQueryService.findByAction(action, limit);
    }

    @GetMapping("/history")
    public List<AuditLogEntryResponse> history(@RequestParam String entityType, @RequestParam String entityId) {
        return auditQueryService.findHistory(resolveClass(entityType), entityId);
    }

    private static Class<?> resolveClass(String entityType) {
        try {
            return Class.forName(entityType);
        } catch (ClassNotFoundException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown entityType: " + entityType);
        }
    }
}
