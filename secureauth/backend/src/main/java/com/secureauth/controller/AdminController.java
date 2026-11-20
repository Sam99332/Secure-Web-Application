package com.secureauth.controller;

import com.secureauth.dto.ApiResponse;
import com.secureauth.dto.PagedResponse;
import com.secureauth.dto.UserAdminDtos.*;
import com.secureauth.entity.AccountStatus;
import com.secureauth.entity.RoleName;
import com.secureauth.security.UserPrincipal;
import com.secureauth.service.AdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin Management", description = "Administrator-only user management, RBAC role assignment, and audit logs")
@SecurityRequirement(name = "cookieAuth")
public class AdminController {

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    @GetMapping("/dashboard")
    @Operation(summary = "Get admin dashboard user metrics and recent registrations")
    public ResponseEntity<ApiResponse<AdminDashboardResponse>> getDashboard() {
        return ResponseEntity.ok(ApiResponse.ok("Dashboard metrics loaded.", adminService.getDashboardOverview()));
    }

    @GetMapping("/users")
    @Operation(summary = "List users with pagination, search, status/role filtering, and sorting")
    public ResponseEntity<ApiResponse<PagedResponse<UserResponse>>> listUsers(
        @RequestParam(required = false) String search,
        @RequestParam(required = false) AccountStatus status,
        @RequestParam(required = false) RoleName role,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "10") int size,
        @RequestParam(defaultValue = "createdAt") String sortBy,
        @RequestParam(defaultValue = "desc") String sortDir
    ) {
        return ResponseEntity.ok(ApiResponse.ok(
            "Users loaded.",
            adminService.listUsers(search, status, role, page, size, sortBy, sortDir)
        ));
    }

    @GetMapping("/users/{id}")
    @Operation(summary = "Get user details by ID")
    public ResponseEntity<ApiResponse<UserResponse>> getUser(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok("User loaded.", adminService.getUserById(id)));
    }

    @PutMapping("/users/{id}")
    @Operation(summary = "Update user account information")
    public ResponseEntity<ApiResponse<UserResponse>> updateUser(
        @AuthenticationPrincipal UserPrincipal admin,
        @PathVariable Long id,
        @Valid @RequestBody AdminUpdateUserRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.ok(
            "User updated.",
            adminService.updateUser(admin.getId(), id, request)
        ));
    }

    @PatchMapping("/users/{id}/status")
    @Operation(summary = "Activate or deactivate a user account")
    public ResponseEntity<ApiResponse<UserResponse>> updateStatus(
        @AuthenticationPrincipal UserPrincipal admin,
        @PathVariable Long id,
        @Valid @RequestBody UpdateStatusRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.ok(
            "User status updated.",
            adminService.updateStatus(admin.getId(), id, request.status())
        ));
    }

    @PatchMapping("/users/{id}/roles")
    @Operation(summary = "Assign USER / ADMIN roles to a user")
    public ResponseEntity<ApiResponse<UserResponse>> updateRoles(
        @AuthenticationPrincipal UserPrincipal admin,
        @PathVariable Long id,
        @Valid @RequestBody UpdateRolesRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.ok(
            "User roles updated.",
            adminService.updateRoles(admin.getId(), id, request.roles())
        ));
    }

    @DeleteMapping("/users/{id}")
    @Operation(summary = "Delete a user account")
    public ResponseEntity<ApiResponse<Void>> deleteUser(
        @AuthenticationPrincipal UserPrincipal admin,
        @PathVariable Long id
    ) {
        adminService.deleteUser(admin.getId(), id);
        return ResponseEntity.ok(ApiResponse.ok("User deleted.", null));
    }

    @GetMapping("/audit-logs")
    @Operation(summary = "Fetch paginated admin audit trail")
    public ResponseEntity<ApiResponse<PagedResponse<AuditLogResponse>>> getAuditLogs(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(ApiResponse.ok("Audit logs loaded.", adminService.getAuditLogs(page, size)));
    }
}
