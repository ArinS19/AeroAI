package com.aeroai.backend.auth.dto;

public record AuthResponse(
        String token,
        String tokenType,
        long expiresInSeconds,
        UserDto user
) {
    public static AuthResponse of(String token, long expiresInSeconds, UserDto user) {
        return new AuthResponse(token, "Bearer", expiresInSeconds, user);
    }
}
