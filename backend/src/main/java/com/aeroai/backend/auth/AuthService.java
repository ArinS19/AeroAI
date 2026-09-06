package com.aeroai.backend.auth;

import com.aeroai.backend.auth.dto.AuthResponse;
import com.aeroai.backend.auth.dto.LoginRequest;
import com.aeroai.backend.auth.dto.RegisterRequest;
import com.aeroai.backend.auth.dto.UserDto;
import com.aeroai.backend.common.exception.ConflictException;
import com.aeroai.backend.common.exception.UnauthorizedException;
import com.aeroai.backend.common.exception.ValidationException;
import com.aeroai.backend.config.AppProperties;
import com.aeroai.backend.tracking.TrackedFlightRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final TrackedFlightRepository trackedFlightRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AppProperties appProperties;

    public AuthService(UserRepository userRepository,
                        TrackedFlightRepository trackedFlightRepository,
                        PasswordEncoder passwordEncoder,
                        JwtService jwtService,
                        AppProperties appProperties) {
        this.userRepository = userRepository;
        this.trackedFlightRepository = trackedFlightRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.appProperties = appProperties;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String normalizedEmail = request.email().trim().toLowerCase();

        if (!request.password().equals(request.confirmPassword())) {
            throw new ValidationException("Password and confirmation password do not match.");
        }
        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new ConflictException("An account with this email already exists.");
        }

        User user = new User(normalizedEmail, passwordEncoder.encode(request.password()));
        user = userRepository.save(user);
        return buildAuthResponse(user);
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        String normalizedEmail = request.email().trim().toLowerCase();
        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new UnauthorizedException("Invalid email or password."));

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new UnauthorizedException("Invalid email or password.");
        }
        return buildAuthResponse(user);
    }

    @Transactional(readOnly = true)
    public UserDto getCurrentUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UnauthorizedException("Session is no longer valid."));
        return toDto(user);
    }

    private AuthResponse buildAuthResponse(User user) {
        String token = jwtService.generateToken(user);
        long expirySeconds = Duration.ofMinutes(appProperties.getJwt().getExpiryMinutes()).toSeconds();
        return AuthResponse.of(token, expirySeconds, toDto(user));
    }

    private UserDto toDto(User user) {
        long trackedCount = trackedFlightRepository.countByUserIdAndActiveTrue(user.getId());
        return new UserDto(user.getId(), user.getEmail(), user.getCreatedAt(), trackedCount);
    }
}
