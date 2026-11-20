package com.secureauth.dto;

import com.secureauth.entity.AccountStatus;
import com.secureauth.entity.LoginStatus;
import com.secureauth.entity.RoleName;
import jakarta.validation.constraints.*;
import java.time.Instant;
import java.util.List;
import java.util.Set;

public class UserAdminDtos {
    public record UserResponse(
        Long id,
        String fullName,
        String username,
        String email,
        String avatarUrl,
        AccountStatus accountStatus,
        Set<String> roles,
        Instant createdAt,
        Instant updatedAt
    ) {}

    public record AuthSessionResponse(
        UserResponse user,
        Instant expiresAt
    ) {}

    public record UpdateProfileRequest(
        @NotBlank @Size(min = 2, max = 120) String fullName,
        @NotBlank @Email @Size(max = 150) String email,
        @Size(max = 512) String avatarUrl
    ) {}

    public record ChangePasswordRequest(
        @NotBlank String currentPassword,
        @NotBlank
        @Pattern(
            regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9]).{8,100}$",
            message = "Password must be 8+ chars with upper, lower, digit, and symbol"
        )
        String newPassword
    ) {}

    public record AdminUpdateUserRequest(
        @NotBlank @Size(min = 2, max = 120) String fullName,
        @NotBlank @Email @Size(max = 150) String email,
        @NotBlank @Pattern(regexp = "^[a-zA-Z0-9._-]{3,50}$") String username
    ) {}

    public record UpdateStatusRequest(
        @NotNull AccountStatus status
    ) {}

    public record UpdateRolesRequest(
        @NotEmpty Set<RoleName> roles
    ) {}

    public record LoginHistoryResponse(
        Long id,
        Instant loginTime,
        String ipAddress,
        LoginStatus loginStatus
    ) {}

    public record AuditLogResponse(
        Long id,
        Long adminId,
        String adminUsername,
        String action,
        Long targetUserId,
        String details,
        Instant timestamp
    ) {}

    public record AdminDashboardResponse(
        long totalUsers,
        long activeUsers,
        long inactiveUsers,
        long adminCount,
        List<UserResponse> recentRegistrations
    ) {}
}
