package com.secureauth.mapper;

import com.secureauth.dto.UserAdminDtos.*;
import com.secureauth.entity.AuditLog;
import com.secureauth.entity.LoginHistory;
import com.secureauth.entity.User;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

@Component
public class UserMapper {

    public UserResponse toUserResponse(User user) {
        return new UserResponse(
            user.getId(),
            user.getFullName(),
            user.getUsername(),
            user.getEmail(),
            user.getAvatarUrl(),
            user.getAccountStatus(),
            user.getRoles().stream()
                .map(r -> r.getRoleName().name())
                .collect(Collectors.toSet()),
            user.getCreatedAt(),
            user.getUpdatedAt()
        );
    }

    public LoginHistoryResponse toLoginHistoryResponse(LoginHistory entry) {
        return new LoginHistoryResponse(
            entry.getId(),
            entry.getLoginTime(),
            entry.getIpAddress(),
            entry.getLoginStatus()
        );
    }

    public AuditLogResponse toAuditLogResponse(AuditLog log) {
        return new AuditLogResponse(
            log.getId(),
            log.getAdmin().getId(),
            log.getAdmin().getUsername(),
            log.getAction(),
            log.getTargetUserId(),
            log.getDetails(),
            log.getTimestamp()
        );
    }
}
