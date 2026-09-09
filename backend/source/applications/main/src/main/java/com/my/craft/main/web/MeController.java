package com.my.craft.main.web;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.my.craft.security.user.CustomAuthenticationToken;
import com.my.craft.security.user.UserContext;

@RestController
@RequestMapping("/me")
public class MeController {

    // UserContextEnrichmentFilter (see SecurityConfig) wraps every authenticated
    // request's Authentication into a CustomAuthenticationToken before it reaches
    // here, regardless of whether the request came in via OIDC session, JWT, or Basic.
    @GetMapping("")
    public UserContext me(Authentication authentication) {
        if (!(authentication instanceof CustomAuthenticationToken customAuthentication)) {
            throw new IllegalStateException(
                    "Expected a CustomAuthenticationToken -- is UserContextEnrichmentFilter registered on this chain?");
        }
        return customAuthentication.getUserContext();
    }
}
