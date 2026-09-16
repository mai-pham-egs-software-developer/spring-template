package com.my.craft.security.authz;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import org.casbin.jcasbin.main.Enforcer;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerExecutionChain;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import com.my.craft.security.annotation.RequiresPermission;
import com.my.craft.security.domain.Organization;
import com.my.craft.security.security.CustomAuthenticationToken;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Bridges {@code authorizeHttpRequests(...).access(...)} to the jcasbin {@link Enforcer}. Builds
 * the exact 5-value tuple {@code rbac_model.conf}'s {@code request_definition} expects:
 *
 * <ul>
 *   <li>{@code userId} -- the enriched {@link com.my.craft.security.security.UserContext#userId()},
 *       the Keycloak subject claim, the same id {@code User.id} uses (not the username: {@code g}
 *       rows assign roles to this id, matching {@code UserOrganizationRole.user}).
 *   <li>{@code objectType}/{@code action} -- {@link RequiresPermission#resource()}/{@link
 *       RequiresPermission#action()} off the resolved handler method, or (if the method itself
 *       doesn't carry one) its declaring class.
 *   <li>{@code objectId} -- the {@code @PathVariable} named by {@link
 *       RequiresPermission#idParam()}, or {@code ""} for a collection-level operation.
 *   <li>{@code orgId} -- the {@code @PathVariable} named by {@link
 *       RequiresPermission#orgIdParam()}, if that's set and present. For a "master-less"
 *       operation -- {@code orgIdParam} unset (a platform-level call with no owning org in its
 *       path, e.g. managing organizations or users themselves), or no {@link RequiresPermission}
 *       at all -- falls back to the {@value #ORG_ID_HEADER} request header if the caller sent
 *       one, so a client can still act within a specific org on an endpoint whose path doesn't
 *       name one; only if that header is absent too does it fall back further to {@link
 *       Organization#MASTER_ID} (the org the {@code SUPER_ADMIN} role {@code
 *       MasterAccountInitializer} grants lives in, so its wildcard permission still covers these
 *       by default).
 * </ul>
 *
 * <p>A handler with no {@link RequiresPermission} anywhere (method or class -- e.g. {@code
 * CasbinPolicyController}, {@code MeController}) falls back to {@code objectType = request
 * path}, {@code objectId = ""}, {@code action = HTTP method}: opt-in per controller/method, not a
 * breaking change for ones that don't use the annotation.
 *
 * <p>{@code exemptPathPatterns} (Ant-style, e.g. {@code "/operators/health/**"}, from {@code
 * casbin.rbac-exempt-paths} in {@code application.yml}) skip all of the above outright -- {@link
 * #check} returns granted before even looking at the annotation or building the tuple, for a
 * path that needs to sit under a {@code BEARER}-protected prefix but should behave like {@code
 * /me} (no org/role/permission check at all, just plain authentication -- still enforced by
 * {@code AuthenticatedAuthorizationManager.authenticated()} alongside this manager in {@code
 * SecurityConfig}'s {@code allOf(...)}).
 *
 * <p>Requires {@link com.my.craft.security.security.UserContextEnrichmentFilter} to have already
 * run, i.e. must only be used on a chain that registers it with {@code addFilterBefore} (not
 * {@code addFilterAfter}) relative to {@code AuthorizationFilter}.
 */
public class CasbinAuthorizationManager implements AuthorizationManager<RequestAuthorizationContext> {

    /** Client-supplied org context for a "master-less" call -- see {@link #resolveOrgId}. */
    public static final String ORG_ID_HEADER = "X-Org-Id";

    private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();

    private final Enforcer enforcer;
    private final RequestMappingHandlerMapping handlerMapping;
    private final List<String> exemptPathPatterns;

    public CasbinAuthorizationManager(
            Enforcer enforcer, RequestMappingHandlerMapping handlerMapping, List<String> exemptPathPatterns) {
        this.enforcer = enforcer;
        this.handlerMapping = handlerMapping;
        this.exemptPathPatterns = exemptPathPatterns;
    }

    @Override
    public AuthorizationDecision check(Supplier<Authentication> authentication, RequestAuthorizationContext context) {
        HttpServletRequest request = context.getRequest();
        if (isExempt(request)) {
            return new AuthorizationDecision(true);
        }

        Authentication auth = authentication.get();
        if (!(auth instanceof CustomAuthenticationToken token)) {
            return new AuthorizationDecision(false);
        }
        String userId = token.getUserContext().userId();
        RequiresPermission permission = resolveAnnotation(request);

        String orgId = resolveOrgId(permission, request);
        String objectType = permission != null ? permission.resource() : request.getRequestURI();
        String objectId = permission != null ? pathVariableOrDefault(request, permission.idParam(), "") : "";
        String action = permission != null ? permission.action() : request.getMethod();

        boolean granted = enforcer.enforce(userId, orgId, objectType, objectId, action);
        return new AuthorizationDecision(granted);
    }

    /**
     * Path-based org wins outright when {@code orgIdParam} names one. Otherwise ("master-less":
     * no {@link RequiresPermission}, or one with {@code orgIdParam} unset) tries the {@value
     * #ORG_ID_HEADER} header the caller sent, and only falls back to {@link
     * Organization#MASTER_ID} if that's absent too.
     */
    private static String resolveOrgId(RequiresPermission permission, HttpServletRequest request) {
        if (permission != null && !permission.orgIdParam().isEmpty()) {
            String fromPath = pathVariableOrDefault(request, permission.orgIdParam(), null);
            if (fromPath != null) {
                return fromPath;
            }
        }
        String fromHeader = request.getHeader(ORG_ID_HEADER);
        return fromHeader != null && !fromHeader.isBlank() ? fromHeader : String.valueOf(Organization.MASTER_ID);
    }

    private boolean isExempt(HttpServletRequest request) {
        String path = request.getRequestURI();
        return exemptPathPatterns.stream().anyMatch(pattern -> PATH_MATCHER.match(pattern, path));
    }

    /** Method-level {@link RequiresPermission} wins over one on the declaring class; {@code
     * null} if neither the resolved handler nor its class carries it (including when no handler
     * resolves at all, e.g. a 404). */
    private RequiresPermission resolveAnnotation(HttpServletRequest request) {
        HandlerMethod handlerMethod = resolveHandlerMethod(request);
        if (handlerMethod == null) {
            return null;
        }
        RequiresPermission methodLevel = handlerMethod.getMethodAnnotation(RequiresPermission.class);
        return methodLevel != null ? methodLevel : handlerMethod.getBeanType().getAnnotation(RequiresPermission.class);
    }

    private HandlerMethod resolveHandlerMethod(HttpServletRequest request) {
        try {
            HandlerExecutionChain chain = handlerMapping.getHandler(request);
            if (chain != null && chain.getHandler() instanceof HandlerMethod handlerMethod) {
                return handlerMethod;
            }
        } catch (Exception e) {
            // No matching handler, or resolution failed -- fall back to the path/method check.
        }
        return null;
    }

    /** {@code getHandler(request)} above (a side effect of resolving the handler, not something
     * called separately) leaves the matched {@code @PathVariable}s on the request under this
     * attribute -- see {@link HandlerMapping#URI_TEMPLATE_VARIABLES_ATTRIBUTE}. */
    @SuppressWarnings("unchecked")
    private static String pathVariableOrDefault(HttpServletRequest request, String name, String defaultValue) {
        if (name.isEmpty()) {
            return defaultValue;
        }
        Map<String, String> pathVariables = (Map<String, String>) request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE);
        String value = pathVariables != null ? pathVariables.get(name) : null;
        return value != null ? value : defaultValue;
    }
}
