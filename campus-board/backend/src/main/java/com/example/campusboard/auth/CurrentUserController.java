package com.example.campusboard.auth;

import com.example.campusboard.user.UserView;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/me")
public class CurrentUserController {
    private final AuthService authService;

    public CurrentUserController(AuthService authService) {
        this.authService = authService;
    }

    @GetMapping
    public UserView currentUser(@AuthenticationPrincipal UUID userId) {
        return authService.currentUser(userId);
    }
}
