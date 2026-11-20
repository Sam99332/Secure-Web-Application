package com.secureauth.dto;

import jakarta.validation.constraints.*;

public class AuthDtos {
    public record RegisterRequest(
        @NotBlank(message = "Full name is required")
        @Size(min = 2, max = 120)
        String fullName,

        @NotBlank(message = "Email is required")
        @Email(message = "Invalid email format")
        @Size(max = 150)
        String email,

        @NotBlank(message = "Username is required")
        @Pattern(regexp = "^[a-zA-Z0-9._-]{3,50}$", message = "Username must be 3-50 alphanumeric characters (._- allowed)")
        String username,

        @NotBlank(message = "Password is required")
        @Pattern(
            regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9]).{8,100}$",
            message = "Password must be 8+ chars and include upper, lower, digit, and special character"
        )
        String password
    ) {}

    public record LoginRequest(
        @NotBlank(message = "Email or username is required")
        String identifier,

        @NotBlank(message = "Password is required")
        String password,

        boolean rememberMe
    ) {}

    public record ForgotPasswordRequest(
        @NotBlank(message = "Email is required")
        @Email(message = "Invalid email format")
        String email
    ) {}

    public record ResetPasswordRequest(
        @NotBlank(message = "Reset token is required")
        String token,

        @NotBlank(message = "New password is required")
        @Pattern(
            regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9]).{8,100}$",
            message = "Password must be 8+ chars and include upper, lower, digit, and special character"
        )
        String newPassword
    ) {}
}
