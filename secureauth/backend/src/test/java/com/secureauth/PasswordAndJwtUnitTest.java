package com.secureauth;

import com.secureauth.dto.UserAdminDtos.UpdateProfileRequest;
import com.secureauth.entity.AccountStatus;
import com.secureauth.entity.Role;
import com.secureauth.entity.RoleName;
import com.secureauth.entity.User;
import com.secureauth.mapper.UserMapper;
import com.secureauth.repository.LoginHistoryRepository;
import com.secureauth.repository.UserRepository;
import com.secureauth.security.JwtTokenProvider;
import com.secureauth.security.LoginRateLimiterService;
import com.secureauth.service.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PasswordAndJwtUnitTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private LoginHistoryRepository loginHistoryRepository;

    @Test
    void bcryptPasswordHashingAndVerification() {
        PasswordEncoder encoder = new BCryptPasswordEncoder(10);
        String hash = encoder.encode("Enterprise@2026!");
        assertNotEquals("Enterprise@2026!", hash);
        assertTrue(hash.startsWith("$2"));
        assertTrue(encoder.matches("Enterprise@2026!", hash));
        assertFalse(encoder.matches("WrongPassword1!", hash));
    }

    @Test
    void jwtTokenValidationAndTamperRejection() {
        JwtTokenProvider provider = new JwtTokenProvider(
            "dGVzdC1zZWNyZXQta2V5LWZvci1qdW5pdC1pbnRlZ3JhdGlvbi10ZXN0cy0xMjM0NTY3ODkw",
            60_000L,
            600_000L
        );
        JwtTokenProvider.GeneratedToken generated = provider.generateToken(
            1L,
            "alex.morgan",
            List.of("ROLE_USER"),
            false
        );
        assertTrue(provider.validateToken(generated.token()));
        assertEquals("alex.morgan", provider.parseClaims(generated.token()).getSubject());
        assertFalse(provider.validateToken(generated.token() + "tampered"));
    }

    @Test
    void rateLimiterLocksAfterFiveFailures() {
        LoginRateLimiterService limiter = new LoginRateLimiterService();
        for (int i = 0; i < 4; i++) {
            assertEquals(0L, limiter.recordFailure("attacker@example.com"));
        }
        assertTrue(limiter.recordFailure("attacker@example.com") > 0);
        assertTrue(limiter.getRemainingLockoutSeconds("attacker@example.com") > 0);
        limiter.reset("attacker@example.com");
        assertEquals(0L, limiter.getRemainingLockoutSeconds("attacker@example.com"));
    }

    @Test
    void userServiceUpdatesOwnProfileSafely() {
        User user = new User();
        user.setId(5L);
        user.setFullName("Old Name");
        user.setUsername("alex.morgan");
        user.setEmail("alex@example.com");
        user.setAccountStatus(AccountStatus.ACTIVE);
        user.setRoles(Set.of(new Role(RoleName.ROLE_USER)));

        when(userRepository.findById(5L)).thenReturn(Optional.of(user));
        when(userRepository.existsByEmailIgnoreCaseAndIdNot("new@example.com", 5L)).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        UserService service = new UserService(userRepository, loginHistoryRepository, new BCryptPasswordEncoder(), new UserMapper());
        var updated = service.updateProfile(5L, new UpdateProfileRequest("Alex Updated", "new@example.com", null));

        assertEquals("Alex Updated", updated.fullName());
        assertEquals("new@example.com", updated.email());
        verify(userRepository).save(user);
    }
}
