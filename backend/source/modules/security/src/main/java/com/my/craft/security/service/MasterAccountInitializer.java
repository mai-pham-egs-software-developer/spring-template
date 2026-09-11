package com.my.craft.security.service;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * Ensures the {@code app.master-account.*} account exists in the identity provider on every
 * boot -- see {@link UserService#initMasterAccount()}. Left to fail startup on error, the same
 * fail-fast stance {@code CasbinPolicySeeder}/{@code CasbinModelConfigInitializer} take for their
 * own one-time bootstrap work, rather than silently leaving a fresh environment with no way to
 * log in as admin.
 */
@Component
public class MasterAccountInitializer implements ApplicationRunner {

    private final UserService userService;

    public MasterAccountInitializer(UserService userService) {
        this.userService = userService;
    }

    @Override
    public void run(ApplicationArguments args) {
        userService.initMasterAccount();
    }
}
