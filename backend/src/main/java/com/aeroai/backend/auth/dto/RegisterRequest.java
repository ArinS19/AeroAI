package com.aeroai.backend.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record RegisterRequest(
        @NotBlank @Email(message = "must be a valid email address") String email,

        @NotBlank
        @Pattern(
                regexp = "^(?=.*[A-Za-z])(?=.*\\d).{8,}$",
                message = "must be at least 8 characters and contain at least one letter and one number"
        )
        String password,

        @NotBlank(message = "must confirm your password") String confirmPassword
) {
}
