package com.secureauth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.secureauth.dto.AuthDtos.*;
import com.secureauth.dto.UserAdminDtos.UpdateStatusRequest;
import com.secureauth.entity.*;
import com.secureauth.repository.RoleRepository;
import com.secureauth.repository.UserRepository;
import com.secureauth.service.AuthService;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Set;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthSecurityIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserRepository userRepository;
    @Autowired private RoleRepository roleRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private AuthService authService;

    private Role userRole;
    private Role adminRole;

    @BeforeEach
    void setup() {
        userRepository.deleteAll();
        userRole = roleRepository.findByRoleName(RoleName.ROLE_USER)
            .orElseGet(() -> roleRepository.save(new Role(RoleName.ROLE_USER)));
        adminRole = roleRepository.findByRoleName(RoleName.ROLE_ADMIN)
            .orElseGet(() -> roleRepository.save(new Role(RoleName.ROLE_ADMIN)));

        User admin = new User();
        admin.setFullName("Admin Tester");
        admin.setUsername("admin");
        admin.setEmail("admin@secureauth.dev");
        admin.setPassword(passwordEncoder.encode("Admin@Pass123!"));
        admin.setAccountStatus(AccountStatus.ACTIVE);
        admin.setRoles(Set.of(userRole, adminRole));
        userRepository.save(admin);
    }

    @Test
    void registerLoginAndEnforceRbac() throws Exception {
        RegisterRequest reg = new RegisterRequest("Sam Lee", "sam@example.com", "sam.lee", "Strong@Pass1!");
        MvcResult regResult = mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(reg)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.success").value(true))
            .andReturn();

        Cookie userCookie = regResult.getResponse().getCookie("SECUREAUTH_ACCESS_TOKEN");
        assertNotNull(userCookie);

        // Duplicate email must fail with 409
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(reg)))
            .andExpect(status().isConflict());

        // User can access own profile
        mockMvc.perform(get("/api/users/me").cookie(userCookie))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.username").value("sam.lee"));

        // User is strictly forbidden from admin API
        mockMvc.perform(get("/api/admin/dashboard").cookie(userCookie))
            .andExpect(status().isForbidden());
    }

    @Test
    void adminCanManageUsersAndAuditLogs() throws Exception {
        User target = new User();
        target.setFullName("Target User");
        target.setUsername("target.user");
        target.setEmail("target@example.com");
        target.setPassword(passwordEncoder.encode("Target@Pass1!"));
        target.setAccountStatus(AccountStatus.ACTIVE);
        target.setRoles(Set.of(userRole));
        target = userRepository.save(target);

        LoginRequest adminLogin = new LoginRequest("admin@secureauth.dev", "Admin@Pass123!", false);
        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(adminLogin)))
            .andExpect(status().isOk())
            .andReturn();

        Cookie adminCookie = loginResult.getResponse().getCookie("SECUREAUTH_ACCESS_TOKEN");
        assertNotNull(adminCookie);

        mockMvc.perform(patch("/api/admin/users/" + target.getId() + "/status")
                .with(csrf())
                .cookie(adminCookie)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new UpdateStatusRequest(AccountStatus.INACTIVE))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.accountStatus").value("INACTIVE"));

        mockMvc.perform(get("/api/admin/audit-logs").cookie(adminCookie))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("STATUS_CHANGED")));
    }

    @Test
    void sqlInjectionPayloadIsRejectedSafely() throws Exception {
        LoginRequest sqli = new LoginRequest("' OR '1'='1' --", "anything", false);
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(sqli)))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void passwordResetFlowWorksAndPreventsReuse() throws Exception {
        String rawToken = authService.requestPasswordReset(new ForgotPasswordRequest("admin@secureauth.dev"));
        assertNotNull(rawToken);

        ResetPasswordRequest resetReq = new ResetPasswordRequest(rawToken, "NewAdmin@Pass99!");
        mockMvc.perform(post("/api/auth/reset-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(resetReq)))
            .andExpect(status().isOk());

        // Reusing same token fails
        mockMvc.perform(post("/api/auth/reset-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(resetReq)))
            .andExpect(status().isBadRequest());
    }
}
