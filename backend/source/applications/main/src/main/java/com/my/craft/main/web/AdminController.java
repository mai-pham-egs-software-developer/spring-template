package com.my.craft.main.web;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    // Principal is an OidcUser for browser session logins (oauth2Login) and a
    // Jwt for Bearer-token API calls (oauth2ResourceServer) -- see SecurityConfig.
    @GetMapping("/test")
    public String test() {
        return "test";
    }
}
