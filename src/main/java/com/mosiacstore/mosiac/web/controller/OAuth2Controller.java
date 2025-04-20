package com.mosiacstore.mosiac.web.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class OAuth2Controller {

    @GetMapping("/login/google")
    public void googleLogin() {
        // This endpoint will be redirected by Spring Security OAuth2 to Google
        // Just a placeholder for frontend to call
    }
}