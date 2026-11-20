package com.secureauth.service;

import com.secureauth.dto.PagedResponse;
import com.secureauth.dto.UserAdminDtos.*;
import com.secureauth.entity.*;
import com.secureauth.exception.BadRequestException;
import com.secureauth.exception.ConflictException;
import com.secureauth.exception.ResourceNotFoundException;
import com.secureauth.mapper.UserMapper;
import com.secureauth.repository.AuditLogRepository;
import com.secureauth.repository.RoleRepository;
import com.secureauth.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class AdminService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final AuditLogRepository auditLogRepository;
    private final UserMapper userMapper;

    public AdminService(
        UserRepository userRepository,
        RoleRepository roleRepository,
        AuditLogRepository auditLogRepository,
        UserMapper userMapper
    ) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.auditLogRepository = auditLogRepository;
        this.userMapper = userMapper;
    }

    @Transactional(readOnly = true)
    public AdminDashboardResponse getDashboardOverview() {
        long total = userRepository.count();
        long active = userRepository.countByAccountStatus(AccountStatus.ACTIVE);
        long inactive = userRepository.countByAccountStatus(AccountStatus.INACTIVE);
        long admins = userRepository.countByRoleName(RoleName.ROLE_ADMIN);
        List<UserResponse> recent = userRepository.findTop5ByOrderByCreatedAtDesc()
            .stream()
            .map(userMapper::toUserResponse)
            .toList();

        return new AdminDashboardResponse(total, active, inactive, admins, recent);
    }

    @Transactional(readOnly = true)
    public PagedResponse<UserResponse> listUsers(
        String search,
        AccountStatus status,
        RoleName roleName,
        int page,
        int size,
        String sortBy,
        String sortDir
    ) {
        String safeSortField = switch (sortBy == null ? "" : sortBy) {
            case "fullName", "username", "email", "accountStatus" -> sortBy;
            default -> "createdAt";
        };
        Sort.Direction dir = "asc".equalsIgnoreCase(sortDir) ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.min(100, Math.max(1, size)), Sort.by(dir, safeSortField));

        Page<User> result = userRepository.searchUsers(
            search == null ? null : search.trim(),
            status,
            roleName,
            pageable
        );

        return new PagedResponse<>(
            result.getContent().stream().map(userMapper::toUserResponse).toList(),
            result.getNumber(),
            result.getSize(),
            result.getTotalElements(),
            result.getTotalPages()
        );
    }

    @Transactional(readOnly = true)
    public UserResponse getUserById(Long id) {
        User user = userRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("User not found."));
        return userMapper.toUserResponse(user);
    }

    @Transactional
    public UserResponse updateUser(Long adminId, Long targetUserId, AdminUpdateUserRequest request) {
        User admin = getUserEntity(adminId);
        User target = getUserEntity(targetUserId);

        String email = request.email().trim().toLowerCase();
        String username = request.username().trim().toLowerCase();

        if (userRepository.existsByEmailIgnoreCaseAndIdNot(email, targetUserId)) {
            throw new ConflictException("Email already assigned to another account.");
        }
        if (userRepository.existsByUsernameIgnoreCaseAndIdNot(username, targetUserId)) {
            throw new ConflictException("Username already assigned to another account.");
        }

        target.setFullName(request.fullName().trim());
        target.setEmail(email);
        target.setUsername(username);
        User saved = userRepository.save(target);

        recordAudit(admin, "USER_UPDATED", targetUserId, "Updated user profile for " + username);
        return userMapper.toUserResponse(saved);
    }

    @Transactional
    public UserResponse updateStatus(Long adminId, Long targetUserId, AccountStatus status) {
        if (adminId.equals(targetUserId) && status != AccountStatus.ACTIVE) {
            throw new BadRequestException("Administrators cannot deactivate their own account.");
        }
        User admin = getUserEntity(adminId);
        User target = getUserEntity(targetUserId);
        AccountStatus previous = target.getAccountStatus();
        target.setAccountStatus(status);
        User saved = userRepository.save(target);

        recordAudit(admin, "STATUS_CHANGED", targetUserId, "Changed status from " + previous + " to " + status);
        return userMapper.toUserResponse(saved);
    }

    @Transactional
    public UserResponse updateRoles(Long adminId, Long targetUserId, Set<RoleName> roleNames) {
        if (adminId.equals(targetUserId) && !roleNames.contains(RoleName.ROLE_ADMIN)) {
            throw new BadRequestException("Administrators cannot revoke their own ADMIN role.");
        }
        User admin = getUserEntity(adminId);
        User target = getUserEntity(targetUserId);

        Set<Role> resolvedRoles = new HashSet<>();
        for (RoleName rn : roleNames) {
            Role role = roleRepository.findByRoleName(rn)
                .orElseThrow(() -> new BadRequestException("Role not found: " + rn));
            resolvedRoles.add(role);
        }
        target.setRoles(resolvedRoles);
        User saved = userRepository.save(target);

        recordAudit(admin, "ROLES_UPDATED", targetUserId, "Updated roles to " + roleNames);
        return userMapper.toUserResponse(saved);
    }

    @Transactional
    public void deleteUser(Long adminId, Long targetUserId) {
        if (adminId.equals(targetUserId)) {
            throw new BadRequestException("Administrators cannot delete their own account.");
        }
        User admin = getUserEntity(adminId);
        User target = getUserEntity(targetUserId);
        recordAudit(admin, "USER_DELETED", targetUserId, "Deleted account " + target.getUsername());
        userRepository.delete(target);
    }

    @Transactional(readOnly = true)
    public PagedResponse<AuditLogResponse> getAuditLogs(int page, int size) {
        Page<AuditLog> logs = auditLogRepository.findAllByOrderByTimestampDesc(
            PageRequest.of(Math.max(0, page), Math.min(100, Math.max(1, size)))
        );
        return new PagedResponse<>(
            logs.getContent().stream().map(userMapper::toAuditLogResponse).toList(),
            logs.getNumber(),
            logs.getSize(),
            logs.getTotalElements(),
            logs.getTotalPages()
        );
    }

    private User getUserEntity(Long id) {
        return userRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("User not found."));
    }

    private void recordAudit(User admin, String action, Long targetUserId, String details) {
        AuditLog log = new AuditLog();
        log.setAdmin(admin);
        log.setAction(action);
        log.setTargetUserId(targetUserId);
        log.setDetails(details);
        auditLogRepository.save(log);
    }
}
