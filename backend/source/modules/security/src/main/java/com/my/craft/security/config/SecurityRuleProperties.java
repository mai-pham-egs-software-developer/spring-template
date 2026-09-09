package com.my.craft.security.config;

/**
 * One entry of the top-level {@code security:} YAML sequence -- binds a path pattern to the
 * auth mechanism required for it. See docs/security.md.
 */
public class SecurityRuleProperties {

    private String path;
    private AuthType authType;
    private String username;
    private String password;

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public AuthType getAuthType() {
        return authType;
    }

    public void setAuthType(AuthType authType) {
        this.authType = authType;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
