package com.secureauth.controller;

import com.secureauth.dto.ApiResponse;
import com.secureauth.dto.AuthDtos.*;
import com.secureauth.dto.UserAdminDtos.AuthSessionResponse;
import com.secureauth.security.JwtAuthenticationFilter;
import com.secureauth.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication", description = "Registration, Login, Logout, and Password Recovery endpoints")
public class AuthController {

    private final AuthService authService;
    private final JwtAuthenticationFilter jwtFilter;
    private final String cookieName;
    private final boolean cookieSecure;
    private final String cookieSameSite;

    public AuthController(
        AuthService authService,
        JwtAuthenticationFilter jwtFilter,
        @Value("${app.cookie.name:SECUREAUTH_ACCESS_TOKEN}") String cookieName,
        @Value("${app.cookie.secure:false}") boolean cookieSecure,
        @Value("${app.cookie.same-site:Lax}") String cookieSameSite
    ) {
        this.authService = authService;
        this.jwtFilter = jwtFilter;
        this.cookieName = cookieName;
        this.cookieSecure = cookieSecure;
        this.cookieSameSite = cookieSameSite;
    }

    @PostMapping("/register")
    @Operation(summary = "Register a new standard USER account")
    public ResponseEntity<ApiResponse<AuthSessionResponse>> register(
        @Valid @RequestBody RegisterRequest request,
        HttpServletRequest httpRequest,
        HttpServletResponse httpResponse
    ) {
        AuthService.AuthCookiePayload payload = authService.register(request, extractClientIp(httpRequest));
        attachAuthCookie(httpResponse, payload.jwtToken(), payload.maxAgeSeconds());
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.ok("Registration successful.", payload.session()));
    }

    @PostMapping("/login")
    @Operation(summary = "Authenticate with email or username and issue HttpOnly JWT cookie")
    public ResponseEntity<ApiResponse<AuthSessionResponse>> login(
        @Valid @RequestBody LoginRequest request,
        HttpServletRequest httpRequest,
        HttpServletResponse httpResponse
    ) {
        AuthService.AuthCookiePayload payload = authService.login(request, extractClientIp(httpRequest));
        attachAuthCookie(httpResponse, payload.jwtToken(), payload.maxAgeSeconds());
        return ResponseEntity.ok(ApiResponse.ok("Authentication successful.", payload.session()));
    }

    @PostMapping("/logout")
    @Operation(summary = "Revoke active JWT and clear authentication cookie")
    public ResponseEntity<ApiResponse<Void>> logout(
        HttpServletRequest request,
        HttpServletResponse response
    ) {
        String token = jwtFilter.resolveToken(request);
        authService.logout(token);
        ResponseCookie cleared = ResponseCookie.from(cookieName, "")
            .httpOnly(true)
            .secure(cookieSecure)
            .sameSite(cookieSameSite)
            .path("/")
            .maxAge(0)
            .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cleared.toString());
        return ResponseEntity.ok(ApiResponse.ok("Signed out securely.", null));
    }

    @PostMapping("/forgot-password")
    @Operation(summary = "Request a time-limited password reset token (anti-enumeration safe)")
    public ResponseEntity<ApiResponse<Void>> forgotPassword(
        @Valid @RequestBody ForgotPasswordRequest request
    ) {
        authService.requestPasswordReset(request);
        return ResponseEntity.ok(ApiResponse.ok(
            "If an active account matches that email, password reset instructions have been issued.",
            null
        ));
    }

    @PostMapping("/reset-password")
    @Operation(summary = "Reset password using a valid single-use token")
    public ResponseEntity<ApiResponse<Void>> resetPassword(
        @Valid @RequestBody ResetPasswordRequest request
    ) {
        authService.resetPassword(request);
        return ResponseEntity.ok(ApiResponse.ok("Password updated successfully.", null));
    }

    private void attachAuthCookie(HttpServletResponse response, String jwt, long maxAgeSeconds) {
        ResponseCookie cookie = ResponseCookie.from(cookieName, jwt)
            .httpOnly(true)
            .secure(cookieSecure)
            .sameSite(cookieSameSite)
            .path("/")
            .maxAge(maxAgeSeconds)
            .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    private String extractClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
