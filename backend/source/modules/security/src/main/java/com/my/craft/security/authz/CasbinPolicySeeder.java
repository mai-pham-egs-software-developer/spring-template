package com.my.craft.security.authz;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.casbin.jcasbin.main.Enforcer;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

/**
 * First boot only (empty {@code casbin_rule} table): reads the bundled {@code rbac_policy.csv}
 * and adds each row straight through {@link Enforcer}'s management API ({@code
 * addPolicy}/{@code addGroupingPolicy}) -- the same DB adapter path {@code CasbinPolicyController}
 * uses for a user-triggered change, not jcasbin's file adapter. With auto-save on (the
 * casbin-spring-boot-starter default), each call persists straight through the JDBC {@code
 * Adapter} casbin-spring-boot-starter built, so the table starts out with the same sample
 * admin/editor/viewer policies the CSV describes, and every later change happens in the DB. See
 * docs/security.md.
 *
 * <p>Ordered after {@link CasbinModelConfigInitializer}: policy rows only make sense against
 * whichever model (bundled default or a previously-saved override) ends up active.
 */
@Component
@Order(2)
public class CasbinPolicySeeder implements ApplicationRunner {

    private final Enforcer enforcer;

    public CasbinPolicySeeder(Enforcer enforcer) {
        this.enforcer = enforcer;
    }

    @Override
    public void run(ApplicationArguments args) throws IOException {
        if (!enforcer.getPolicy().isEmpty() || !enforcer.getGroupingPolicy().isEmpty()) {
            return;
        }
        for (String line : readSeedLines()) {
            List<String> tokens = splitRow(line);
            String rowType = tokens.remove(0);
            switch (rowType) {
                case "p" -> enforcer.addPolicy(tokens);
                case "g" -> enforcer.addGroupingPolicy(tokens);
                default -> throw new IllegalStateException("Unknown casbin seed row type: " + rowType);
            }
        }
    }

    private static List<String> readSeedLines() throws IOException {
        List<String> lines = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                new ClassPathResource("casbin/rbac_policy.csv").getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.strip();
                if (!line.isEmpty() && !line.startsWith("#")) {
                    lines.add(line);
                }
            }
        }
        return lines;
    }

    private static List<String> splitRow(String line) {
        List<String> tokens = new ArrayList<>();
        for (String token : line.split(",")) {
            tokens.add(token.strip());
        }
        return tokens;
    }
}
