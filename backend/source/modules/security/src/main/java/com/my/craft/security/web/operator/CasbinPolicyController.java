package com.my.craft.security.web.operator;

import java.util.List;

import org.casbin.jcasbin.main.Enforcer;
import org.casbin.jcasbin.model.Model;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.my.craft.security.authz.CasbinModelConfigInitializer;
import com.my.craft.security.dto.ModelConfigView;
import com.my.craft.security.dto.PolicyRow;
import com.my.craft.security.dto.PolicyRowUpdate;
import com.my.craft.security.dto.UpdateModelConfigRequest;
import com.my.craft.security.domain.operator.CasbinModelConfig;
import com.my.craft.security.repository.CasbinModelConfigJpaRepository;

/**
 * Runtime RBAC management: every policy call here goes through {@link Enforcer}'s management API,
 * which both updates the in-memory policy this instance enforces immediately AND persists the
 * change via casbin-spring-boot-starter's JDBC {@code Adapter} (table {@code casbin_rule}) -- the
 * supported way to change policy without a redeploy. See docs/security.md.
 *
 * <p>This controller's own path ({@code /operators/casbin/*}) is itself BEARER + RBAC protected
 * (see {@code application.yml}'s {@code security:} list) -- reaching it needs a matching {@code
 * p} row already in {@code casbin_rule} for the caller's {@code userId}/{@code orgId}. There is
 * no bundled seed file, so on a fresh DB that row has to be inserted directly, matching {@code
 * rbac_model.conf}'s 5-field {@code p = role, orgId, objectType, objectId, action} (this
 * controller's own {@code objectType} is {@code "casbin"}, unannotated, so it actually falls
 * back to the raw request path -- see {@code CasbinAuthorizationManager}): e.g. {@code p, admin,
 * 0, /operators/casbin/*, *, *} plus a {@code g} row assigning some subject the {@code admin}
 * role within org {@code 0} -- see docs/security.md. In a real app, prefer a dedicated {@code
 * casbin-admin} role over reusing {@code admin} for this.
 *
 * <p><b>Policy rows ({@code /policies}) are dynamic by {@code ptype}</b> rather than one endpoint
 * per row shape: {@code rbac_model.conf} only defines {@code p} (grants) and {@code g} (role
 * assignments) today, but jcasbin lets a model define further sections ({@code p2}, {@code g2},
 * ...), each with its own column count/meaning. A ptype starting with {@code g} is a role/grouping
 * assertion (dispatched to the {@code *GroupingPolicy} methods); anything else is a plain policy
 * assertion (dispatched to the {@code *Policy} methods) -- the same split jcasbin itself uses
 * internally between the {@code p} and {@code g} sections of the model.
 */
@RestController
@RequestMapping(Constants.BASE_PATH + "/casbin")
public class CasbinPolicyController {

    private final Enforcer enforcer;
    private final CasbinModelConfigJpaRepository modelConfigRepository;

    public CasbinPolicyController(Enforcer enforcer, CasbinModelConfigJpaRepository modelConfigRepository) {
        this.enforcer = enforcer;
        this.modelConfigRepository = modelConfigRepository;
    }

    /** Every row of the given {@code ptype} (defaults to {@code p}, e.g. {@code ?ptype=g}). */
    @GetMapping("/policies")
    public List<List<String>> listPolicies(@RequestParam(defaultValue = "p") String ptype) {
        return isGroupingType(ptype) ? enforcer.getNamedGroupingPolicy(ptype) : enforcer.getNamedPolicy(ptype);
    }

    @PostMapping("/policies")
    public void addPolicy(@RequestBody PolicyRow row) {
        if (isGroupingType(row.ptype())) {
            enforcer.addNamedGroupingPolicy(row.ptype(), row.params());
        } else {
            enforcer.addNamedPolicy(row.ptype(), row.params());
        }
    }

    @PutMapping("/policies")
    public void updatePolicy(@RequestBody PolicyRowUpdate update) {
        if (isGroupingType(update.ptype())) {
            enforcer.updateNamedGroupingPolicy(update.ptype(), update.oldParams(), update.newParams());
        } else {
            enforcer.updateNamedPolicy(update.ptype(), update.oldParams(), update.newParams());
        }
    }

    @DeleteMapping("/policies")
    public void removePolicy(@RequestBody PolicyRow row) {
        if (isGroupingType(row.ptype())) {
            enforcer.removeNamedGroupingPolicy(row.ptype(), row.params());
        } else {
            enforcer.removeNamedPolicy(row.ptype(), row.params());
        }
    }

    /**
     * Re-reads every row from {@code casbin_rule}. Only needed to pick up a change made by
     * writing to the table directly (e.g. via psql) instead of through this controller -- calls
     * through {@link Enforcer}'s management API above are already live without it.
     */
    @PostMapping("/reload")
    public void reload() {
        enforcer.loadPolicy();
    }

    /** The RBAC model definition (the {@code userId, orgId, objectType, objectId, action} /
     * matcher text), table {@code casbin_model_config} -- see {@link CasbinModelConfigInitializer}
     * for how it gets there. */
    @GetMapping("/config")
    public ModelConfigView getConfig() {
        return new ModelConfigView(modelConfigOrThrow().getContent());
    }

    /**
     * Replaces the model wholesale: parses {@code content} into a fresh {@link Model}, swaps it
     * into the already-running {@link Enforcer} ({@link Enforcer#setModel}), then {@link
     * Enforcer#loadPolicy()} re-reads {@code casbin_rule} against it (existing policy rows are
     * untouched -- only the matcher/section structure they're evaluated against changes) -- and
     * only then persists {@code content} to {@code casbin_model_config}, so a syntactically broken
     * model (an unchecked {@link RuntimeException} from jcasbin's own parser) fails the request
     * without leaving a bad definition stored for the next restart to load.
     */
    @PutMapping("/config")
    public ModelConfigView updateConfig(@RequestBody UpdateModelConfigRequest request) {
        Model model = new Model();
        model.loadModelFromText(request.content());
        enforcer.setModel(model);
        enforcer.loadPolicy();

        CasbinModelConfig entity = modelConfigOrThrow();
        entity.setContent(request.content());
        modelConfigRepository.save(entity);
        return getConfig();
    }

    private CasbinModelConfig modelConfigOrThrow() {
        return modelConfigRepository
                .findById(CasbinModelConfig.DEFAULT_ID)
                .orElseThrow(() -> new IllegalStateException(
                        "casbin model config missing -- is CasbinModelConfigInitializer registered?"));
    }

    private static boolean isGroupingType(String ptype) {
        return ptype.startsWith("g");
    }
}
