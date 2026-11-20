package com.secureauth.service;

import com.secureauth.dto.UserAdminDtos.*;
import com.secureauth.entity.User;
import com.secureauth.exception.BadRequestException;
import com.secureauth.exception.ConflictException;
import com.secureauth.exception.ResourceNotFoundException;
import com.secureauth.mapper.UserMapper;
import com.secureauth.repository.LoginHistoryRepository;
import com.secureauth.repository.UserRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final LoginHistoryRepository loginHistoryRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;

    public UserService(
        UserRepository userRepository,
        LoginHistoryRepository loginHistoryRepository,
        PasswordEncoder passwordEncoder,
        UserMapper userMapper
    ) {
        this.userRepository = userRepository;
        this.loginHistoryRepository = loginHistoryRepository;
        this.passwordEncoder = passwordEncoder;
        this.userMapper = userMapper;
    }

    @Transactional(readOnly = true)
    public UserResponse getProfile(Long userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User account not found."));
        return userMapper.toUserResponse(user);
    }

    @Transactional
    public UserResponse updateProfile(Long userId, UpdateProfileRequest request) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User account not found."));

        String cleanEmail = request.email().trim().toLowerCase();
        if (userRepository.existsByEmailIgnoreCaseAndIdNot(cleanEmail, userId)) {
            throw new ConflictException("Email address is already in use.");
        }

        user.setFullName(request.fullName().trim());
        user.setEmail(cleanEmail);
        if (request.avatarUrl() != null) {
            user.setAvatarUrl(request.avatarUrl().trim().isEmpty() ? null : request.avatarUrl().trim());
        }

        return userMapper.toUserResponse(userRepository.save(user));
    }

    @Transactional
    public void changePassword(Long userId, ChangePasswordRequest request) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User account not found."));

        if (!passwordEncoder.matches(request.currentPassword(), user.getPassword())) {
            throw new BadRequestException("Current password is incorrect.");
        }
        user.setPassword(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);
    }

    @Transactional(readOnly = true)
    public List<LoginHistoryResponse> getOwnActivity(Long userId) {
        return loginHistoryRepository.findByUserIdOrderByLoginTimeDesc(userId, PageRequest.of(0, 50))
            .stream()
            .map(userMapper::toLoginHistoryResponse)
            .toList();
    }
}
