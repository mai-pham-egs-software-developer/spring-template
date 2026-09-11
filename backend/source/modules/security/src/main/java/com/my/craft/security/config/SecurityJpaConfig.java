package com.my.craft.security.config;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * Scans/enables this module's own {@code models} entities + {@code repository} repositories --
 * {@code @EntityScan}/{@code @EnableJpaRepositories} don't follow a consuming app's widened
 * {@code @SpringBootApplication(scanBasePackages = ...)} the way plain {@code @Component} beans
 * do, the same reason {@code file-storage}'s {@code FileStorageConfig} declares its own. The
 * {@code casbin_rule} table has no entry here -- it's owned entirely by casbin-spring-boot-starter
 * (see the {@code casbin:} block in {@code application.yml} and docs/security.md), not Hibernate.
 */
@Configuration
@EntityScan(basePackages = "com.my.craft.security.models")
@EnableJpaRepositories(basePackages = "com.my.craft.security.repository")
public class SecurityJpaConfig {}
