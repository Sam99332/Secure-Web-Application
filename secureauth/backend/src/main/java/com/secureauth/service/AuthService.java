package com.secureauth.service;

import com.secureauth.dto.AuthDtos.*;
import com.secureauth.dto.UserAdminDtos.AuthSessionResponse;
import com.secureauth.entity.*;
import com.secureauth.exception.BadRequestException;
import com.secureauth.exception.ConflictException;
import com.secureauth.exception.RateLimitExceededException;
import com.secureauth.mapper.UserMapper;
import com.secureauth.repository.*;
import com.secureauth.security.JwtTokenProvider;
import com.secureauth.security.LoginRateLimiterService;
import io.jsonwebtoken.Claims;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Set;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordResetTokenRepository resetTokenRepository;
    private final LoginHistoryRepository loginHistoryRepository;
    private final RevokedTokenRepository revokedTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final LoginRateLimiterService rateLimiter;
    private final UserMapper userMapper;

    public AuthService(
        UserRepository userRepository,
        RoleRepository roleRepository,
        PasswordResetTokenRepository resetTokenRepository,
        LoginHistoryRepository loginHistoryRepository,
        RevokedTokenRepository revokedTokenRepository,
        PasswordEncoder passwordEncoder,
        JwtTokenProvider jwtTokenProvider,
        LoginRateLimiterService rateLimiter,
        UserMapper userMapper
    ) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.resetTokenRepository = resetTokenRepository;
        this.loginHistoryRepository = loginHistoryRepository;
        this.revokedTokenRepository = revokedTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
        this.rateLimiter = rateLimiter;
        this.userMapper = userMapper;
    }

    public record AuthCookiePayload(AuthSessionResponse session, String jwtToken, long maxAgeSeconds) {}

    @Transactional
    public AuthCookiePayload register(RegisterRequest request, String clientIp) {
        String email = request.email().trim().toLowerCase();
        String username = request.username().trim().toLowerCase();

        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new ConflictException("An account with this email is already registered.");
        }
        if (userRepository.existsByUsernameIgnoreCase(username)) {
            throw new ConflictException("This username is already taken.");
        }

        Role userRole = roleRepository.findByRoleName(RoleName.ROLE_USER)
            .orElseGet(() -> roleRepository.save(new Role(RoleName.ROLE_USER)));

        User user = new User();
        user.setFullName(request.fullName().trim());
        user.setEmail(email);
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setAccountStatus(AccountStatus.ACTIVE);
        user.setRoles(Set.of(userRole));
        User saved = userRepository.save(user);

        recordLogin(saved, clientIp, LoginStatus.SUCCESS);
        return issueSession(saved, false);
    }

    @Transactional
    public AuthCookiePayload login(LoginRequest request, String clientIp) {
        String identifier = request.identifier().trim();
        long lockoutRemaining = rateLimiter.getRemainingLockoutSeconds(identifier);
        if (lockoutRemaining > 0) {
            throw new RateLimitExceededException(
                "Too many failed login attempts. Try again in " + lockoutRemaining + " seconds.",
                lockoutRemaining
            );
        }

        User user = userRepository.findByEmailOrUsername(identifier).orElse(null);
        if (user == null || !passwordEncoder.matches(request.password(), user.getPassword())) {
            if (user != null) {
                recordLogin(user, clientIp, LoginStatus.FAILED);
            }
            long lockedSec = rateLimiter.recordFailure(identifier);
            if (lockedSec > 0) {
                throw new RateLimitExceededException(
                    "Account temporarily rate-limited due to failed attempts. Retry in " + lockedSec + " seconds.",
                    lockedSec
                );
            }
            // Anti-enumeration: identical message whether user exists or password mismatches
            throw new BadCredentialsException("Invalid credentials or account unavailable.");
        }

        if (user.getAccountStatus() != AccountStatus.ACTIVE) {
            recordLogin(user, clientIp, LoginStatus.INACTIVE_BLOCKED);
            throw new BadCredentialsException("Invalid credentials or account unavailable.");
        }

        rateLimiter.reset(identifier);
        recordLogin(user, clientIp, LoginStatus.SUCCESS);
        return issueSession(user, request.rememberMe());
    }

    @Transactional
    public void logout(String token) {
        if (token == null || token.isBlank()) return;
        if (jwtTokenProvider.validateToken(token)) {
            Claims claims = jwtTokenProvider.parseClaims(token);
            String jti = claims.getId();
            if (jti != null) {
                revokedTokenRepository.save(new RevokedToken(jti, claims.getExpiration().toInstant()));
            }
        }
        revokedTokenRepository.deleteByExpiresAtBefore(Instant.now());
    }

    @Transactional
    public String requestPasswordReset(ForgotPasswordRequest request) {
        String email = request.email().trim().toLowerCase();
        User user = userRepository.findByEmailIgnoreCase(email).orElse(null);
        if (user == null || user.getAccountStatus() != AccountStatus.ACTIVE) {
            // Anti-enumeration: caller receives generic 200 regardless
            return null;
        }
        resetTokenRepository.invalidateActiveTokensForUser(user.getId());

        byte[] rawBytes = new byte[24];
        SECURE_RANDOM.nextBytes(rawBytes);
        String rawToken = Base64.getUrlEncoder().withoutPadding().encodeToString(rawBytes);

        PasswordResetToken entity = new PasswordResetToken();
        entity.setUser(user);
        entity.setTokenHash(sha256Hex(rawToken));
        entity.setExpiryDate(Instant.now().plusSeconds(900)); // 15 min expiry
        entity.setUsed(false);
        resetTokenRepository.save(entity);

        // In production, dispatch via SMTP/email provider; never log raw tokens in production logs
        log.info("Issued 15-minute password reset token for user id={}", user.getId());
        return rawToken;
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        String tokenHash = sha256Hex(request.token().trim());
        PasswordResetToken resetToken = resetTokenRepository.findByTokenHash(tokenHash)
            .orElseThrow(() -> new BadRequestException("Invalid or expired password reset token."));

        if (resetToken.isUsed() || resetToken.getExpiryDate().isBefore(Instant.now())) {
            throw new BadRequestException("Invalid or expired password reset token.");
        }

        User user = resetToken.getUser();
        user.setPassword(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);

        resetToken.setUsed(true);
        resetTokenRepository.save(resetToken);
    }

    private AuthCookiePayload issueSession(User user, boolean rememberMe) {
        List<String> roles = user.getRoles().stream()
            .map(r -> r.getRoleName().name())
            .toList();
        JwtTokenProvider.GeneratedToken generated = jwtTokenProvider.generateToken(
            user.getId(),
            user.getUsername(),
            roles,
            rememberMe
        );
        long maxAge = Math.max(60, generated.expiresAt().getEpochSecond() - Instant.now().getEpochSecond());
        return new AuthCookiePayload(
            new AuthSessionResponse(userMapper.toUserResponse(user), generated.expiresAt()),
            generated.token(),
            maxAge
        );
    }

    private void recordLogin(User user, String ipAddress, LoginStatus status) {
        LoginHistory entry = new LoginHistory();
        entry.setUser(user);
        entry.setIpAddress(ipAddress == null || ipAddress.isBlank() ? "unknown" : ipAddress);
        entry.setLoginStatus(status);
        loginHistoryRepository.save(entry);
    }

    public static String sha256Hex(String input) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(64);
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }
}
