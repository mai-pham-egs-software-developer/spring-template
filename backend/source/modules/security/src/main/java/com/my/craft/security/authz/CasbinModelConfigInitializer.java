package com.my.craft.security.authz;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.casbin.jcasbin.main.Enforcer;
import org.casbin.jcasbin.model.Model;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import com.my.craft.security.domain.operator.CasbinModelConfig;
import com.my.craft.security.repository.CasbinModelConfigJpaRepository;

/**
 * Makes the RBAC model definition itself DB-backed (table {@code casbin_model_config}), editable
 * via {@code CasbinPolicyController}'s {@code /config} endpoint, instead of only the fixed
 * classpath {@code rbac_model.conf} casbin-spring-boot-starter loaded the {@link Enforcer} with at
 * construction:
 *
 * <ul>
 *   <li>First boot (table empty): the {@link Enforcer} already has the classpath model, so this
 *       just persists that same text as the row -- from here on it's the source of truth.
 *   <li>Every later boot: the stored row (possibly edited since) is parsed into a fresh {@link
 *       Model} and applied via {@link Enforcer#setModel}, then {@link Enforcer#loadPolicy()}
 *       re-reads {@code casbin_rule} against it -- otherwise a runtime {@code PUT /config} would
 *       be lost on every restart.
 * </ul>
 */
@Component
public class CasbinModelConfigInitializer implements ApplicationRunner {

    private final Enforcer enforcer;
    private final CasbinModelConfigJpaRepository repository;

    public CasbinModelConfigInitializer(Enforcer enforcer, CasbinModelConfigJpaRepository repository) {
        this.enforcer = enforcer;
        this.repository = repository;
    }

    @Override
    public void run(ApplicationArguments args) throws IOException {
        CasbinModelConfig existing = repository.findById(CasbinModelConfig.DEFAULT_ID).orElse(null);
        if (existing == null) {
            repository.save(new CasbinModelConfig(CasbinModelConfig.DEFAULT_ID, readClasspathModel()));
            return;
        }
        Model model = new Model();
        model.loadModelFromText(existing.getContent());
        enforcer.setModel(model);
        enforcer.loadPolicy();
    }

    private static String readClasspathModel() throws IOException {
        try (InputStream in = new ClassPathResource("casbin/rbac_model.conf").getInputStream()) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
