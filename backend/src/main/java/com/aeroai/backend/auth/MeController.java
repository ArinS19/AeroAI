package com.aeroai.backend.auth;

import com.aeroai.backend.auth.dto.UserDto;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Section 39.3 - Account. Protected endpoint; identity comes only from the
 * validated JWT principal (Section 40), never from a client-supplied ID.
 */
@RestController
@RequestMapping("/api/me")
public class MeController {

    private final AuthService authService;

    public MeController(AuthService authService) {
        this.authService = authService;
    }

    @GetMapping
    public UserDto me(@AuthenticationPrincipal Long userId) {
        return authService.getCurrentUser(userId);
    }
}
