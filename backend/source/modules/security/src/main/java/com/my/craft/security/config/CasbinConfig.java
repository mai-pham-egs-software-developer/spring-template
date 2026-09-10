package com.my.craft.security.config;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import org.casbin.jcasbin.main.Enforcer;
import org.casbin.jcasbin.model.Model;
import org.casbin.jcasbin.persist.file_adapter.FileAdapter;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import com.my.craft.security.authz.jpa.CasbinRuleJpaRepository;
import com.my.craft.security.authz.jpa.JpaCasbinRuleAdapter;

/**
 * Builds the jcasbin {@link Enforcer}: the RBAC model ({@code casbin/rbac_model.conf}, classpath)
 * stays fixed at startup, but policy itself is DB-backed via {@link JpaCasbinRuleAdapter} (table
 * {@code casbin_rule}) so it can change at runtime -- see docs/security.md.
 *
 * <p>Also scans/enables the module's own {@code authz.jpa} entity + repository, the same reason
 * {@code file-storage}'s {@code FileStorageConfig} does: {@code @EntityScan}/{@code
 * @EnableJpaRepositories} don't follow a consuming app's widened {@code
 * @SpringBootApplication(scanBasePackages = ...)} the way plain {@code @Component} beans do.
 */
@Configuration
@EntityScan(basePackages = "com.my.craft.security.authz.jpa")
@EnableJpaRepositories(basePackages = "com.my.craft.security.authz.jpa")
public class CasbinConfig {

    @Bean
    public Enforcer casbinEnforcer(CasbinRuleJpaRepository repository, JpaCasbinRuleAdapter adapter) throws IOException {
        seedPolicyIfEmpty(repository, adapter);
        File modelFile = extractToTempFile("casbin/rbac_model.conf", "rbac-model", ".conf");
        return new Enforcer(modelFile.getAbsolutePath(), adapter);
    }

    // First boot only (empty casbin_rule table): parse the bundled rbac_policy.csv with jcasbin's
    // own FileAdapter into a throwaway Model, then persist it through the JPA adapter -- so the
    // table starts out with the same sample admin/editor/viewer policies the CSV used to seed
    // directly, and every later change happens in the DB.
    private static void seedPolicyIfEmpty(CasbinRuleJpaRepository repository, JpaCasbinRuleAdapter adapter) throws IOException {
        if (repository.count() > 0) {
            return;
        }
        Model seedModel = new Model();
        seedModel.loadModelFromText(readClasspathResource("casbin/rbac_model.conf"));
        try (InputStream policyStream = new ClassPathResource("casbin/rbac_policy.csv").getInputStream()) {
            new FileAdapter(policyStream).loadPolicy(seedModel);
        }
        adapter.savePolicy(seedModel);
    }

    private static String readClasspathResource(String location) throws IOException {
        try (InputStream in = new ClassPathResource(location).getInputStream()) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static File extractToTempFile(String classpathLocation, String prefix, String suffix) throws IOException {
        Path tempFile = Files.createTempFile(prefix, suffix);
        tempFile.toFile().deleteOnExit();
        try (InputStream in = new ClassPathResource(classpathLocation).getInputStream()) {
            Files.copy(in, tempFile, StandardCopyOption.REPLACE_EXISTING);
        }
        return tempFile.toFile();
    }
}
