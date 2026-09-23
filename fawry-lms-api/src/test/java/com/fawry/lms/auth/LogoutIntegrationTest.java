package com.fawry.lms.auth;

import com.fawry.lms.security.JwtTokenProvider;
import com.fawry.lms.user.Role;
import com.fawry.lms.user.User;
import com.fawry.lms.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
class LogoutIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtTokenProvider tokenProvider;

    @Autowired
    private UserRepository userRepository;

    @Test
    void authenticatedLogoutReturnsNoContent() throws Exception {
        User user = createActiveUser();
        String accessToken = tokenProvider.generateAccessToken(user.getId(), user.getRole());
        String refreshToken = tokenProvider.generateRefreshToken(user.getId(), user.getRole());
        user.setAccessToken(accessToken);
        userRepository.saveAndFlush(user);

        mockMvc.perform(post("/api/auth/logout")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isNoContent());

        assertThat(userRepository.findById(user.getId()).orElseThrow().getAccessToken()).isNull();

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType("application/json")
                        .content("{\"refreshToken\":\"" + refreshToken + "\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void logoutRequiresAuthentication() throws Exception {
        mockMvc.perform(post("/api/auth/logout"))
                .andExpect(status().isUnauthorized());
    }

    private User createActiveUser() {
        User user = new User();
        user.setFullName("Logout User");
        user.setEmail("logout-" + UUID.randomUUID() + "@example.com");
        user.setPassword("not-used-for-logout");
        user.setRole(Role.STUDENT);
        user.setActive(true);
        return userRepository.saveAndFlush(user);
    }
}
