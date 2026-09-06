package com.aeroai.backend.auth;

import com.aeroai.backend.auth.dto.AuthResponse;
import com.aeroai.backend.auth.dto.LoginRequest;
import com.aeroai.backend.auth.dto.RegisterRequest;
import com.aeroai.backend.auth.dto.UserDto;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Section 39 - Authentication (email + password only, no OAuth).
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    /**
     * JWT is stateless; there is no server-side token store to invalidate.
     * Logout is a client-side action (discard the token). This endpoint
     * exists so the frontend has a symmetric, explicit call to make and a
     * place to hook future server-side invalidation if ever required.
     */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout() {
        return ResponseEntity.noContent().build();
    }
}
