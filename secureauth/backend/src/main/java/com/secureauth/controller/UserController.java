package com.secureauth.controller;

import com.secureauth.dto.ApiResponse;
import com.secureauth.dto.UserAdminDtos.*;
import com.secureauth.security.UserPrincipal;
import com.secureauth.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@Tag(name = "User Self-Service", description = "Authenticated user profile, password, and login activity endpoints")
@SecurityRequirement(name = "cookieAuth")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/me")
    @Operation(summary = "Get current authenticated user profile")
    public ResponseEntity<ApiResponse<UserResponse>> getMe(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.ok("Profile loaded.", userService.getProfile(principal.getId())));
    }

    @PutMapping("/me")
    @Operation(summary = "Update current authenticated user profile")
    public ResponseEntity<ApiResponse<UserResponse>> updateMe(
        @AuthenticationPrincipal UserPrincipal principal,
        @Valid @RequestBody UpdateProfileRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.ok("Profile updated.", userService.updateProfile(principal.getId(), request)));
    }

    @PutMapping("/me/password")
    @Operation(summary = "Change password for current authenticated user")
    public ResponseEntity<ApiResponse<Void>> changePassword(
        @AuthenticationPrincipal UserPrincipal principal,
        @Valid @RequestBody ChangePasswordRequest request
    ) {
        userService.changePassword(principal.getId(), request);
        return ResponseEntity.ok(ApiResponse.ok("Password updated successfully.", null));
    }

    @GetMapping("/me/activity")
    @Operation(summary = "Fetch personal login history for current user")
    public ResponseEntity<ApiResponse<List<LoginHistoryResponse>>> getMyActivity(
        @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(ApiResponse.ok("Activity loaded.", userService.getOwnActivity(principal.getId())));
    }
}
