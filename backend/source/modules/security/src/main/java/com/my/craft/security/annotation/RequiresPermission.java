package com.my.craft.security.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Declares the {@code objectType}/{@code action} pair a controller (class-level, a default for
 * every one of its methods) or a specific controller method (overrides the class-level one
 * entirely -- see {@code CasbinAuthorizationManager}) represents, mirroring {@link
 * com.my.craft.security.domain.Permission}'s own {@code resource}/{@code actionType} fields.
 *
 * <p>{@code idParam}, when set, names the {@code @PathVariable} carrying the specific resource
 * instance's id (e.g. {@code "organizationId"}); {@code orgIdParam}, when set, names the {@code
 * @PathVariable} carrying the owning organization's id. Both feed {@code
 * com.my.craft.security.authz.CasbinAuthorizationManager}'s {@code rbac_model.conf} request
 * tuple ({@code userId, orgId, objectType, objectId, action}) directly. Leave {@code idParam}
 * unset for a collection-level operation with no single instance; leave {@code orgIdParam} unset
 * for a platform-level operation with no owning org (it then defaults to {@code
 * Organization#MASTER_ID} -- see {@code CasbinAuthorizationManager}).
 *
 * <p>{@code CasbinAuthorizationManager} resolves the handler method for the current request via
 * {@code RequestMappingHandlerMapping} and looks for this annotation there first, then on its
 * declaring class (method wins outright if present -- the two are never merged field-by-field,
 * so a method overriding e.g. {@code action} must repeat {@code orgIdParam}/{@code idParam} too
 * if it needs them); if neither carries it, enforcement falls back to the original
 * path+HTTP-method-based object/action, so this is opt-in per controller/method, not a breaking
 * change for ones that don't use it yet.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface RequiresPermission {

    /** Matches {@code Permission.resource}, e.g. {@code "organization"}. */
    String resource();

    /** Matches {@code Permission.actionType}, e.g. {@code "READ"}, {@code "WRITE"}, {@code "DELETE"}. */
    String action();

    /** Name of the {@code @PathVariable} holding the resource instance id, or {@code ""}
     * (default) for collection-level operations with no single instance. */
    String idParam() default "";

    /** Name of the {@code @PathVariable} holding the owning organization's id, or {@code ""}
     * (default) for a platform-level operation with no owning org. */
    String orgIdParam() default "";
}
