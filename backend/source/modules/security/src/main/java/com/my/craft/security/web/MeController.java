package com.my.craft.security.web;

import com.my.craft.security.security.UserContext;
import com.my.craft.security.security.UserContextUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/me")
public class MeController {

    // UserContextEnrichmentFilter (see SecurityConfig) wraps every authenticated
    // request's Authentication into a CustomAuthenticationToken before it reaches
    // here, regardless of whether the request came in via OIDC session, JWT, or Basic.
    @GetMapping("")
    public UserContext me() {
        return UserContextUtils.currentUserContext();
    }
}
