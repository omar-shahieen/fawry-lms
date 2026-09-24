package com.fawry.lms.auth;

import com.jayway.jsonpath.JsonPath;
import com.fawry.lms.security.JwtTokenProvider;
import com.fawry.lms.user.entities.Role;
import com.fawry.lms.user.entities.User;
import com.fawry.lms.user.repositories.UserRepository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Duration;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
class RefreshIntegrationTest {

    private static final String TEST_SECRET = "test-only-jwt-secret-with-at-least-32-bytes";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtTokenProvider tokenProvider;

    @Test
    void validRefreshTokenReturnsOnlyANewAccessToken() throws Exception {
        User user = createUser(true);
        String refreshToken = tokenProvider.generateRefreshToken(user.getId(), user.getRole());
        user.setAccessToken(tokenProvider.generateAccessToken(user.getId(), user.getRole()));
        userRepository.saveAndFlush(user);

        String response = mockMvc.perform(post("/api/auth/refresh").contentType("application/json")
                .content(refreshBody(refreshToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").doesNotExist())
                .andExpect(jsonPath("$.user").doesNotExist())
                .andReturn().getResponse().getContentAsString();

        String accessToken = JsonPath.read(response, "$.accessToken");
        JwtTokenProvider.TokenClaims claims = tokenProvider.parseToken(accessToken);
        assertThat(claims.tokenType()).isEqualTo(JwtTokenProvider.TokenType.ACCESS);
        assertThat(claims.userId()).isEqualTo(user.getId());
        assertThat(claims.role()).isEqualTo(user.getRole());
    }

    @Test
    void invalidTokenReturnsUnauthorized() throws Exception {
        mockMvc.perform(post("/api/auth/refresh").contentType("application/json")
                .content(refreshBody("not-a-valid-token")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void expiredRefreshTokenReturnsUnauthorized() throws Exception {
        User user = createUser(true);
        JwtTokenProvider expiredProvider = new JwtTokenProvider(
                TEST_SECRET, Duration.ofMinutes(15), Duration.ofSeconds(-1));
        String expiredToken = expiredProvider.generateRefreshToken(user.getId(), user.getRole());

        mockMvc.perform(post("/api/auth/refresh").contentType("application/json")
                .content(refreshBody(expiredToken)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void accessTokenCannotBeUsedAsRefreshToken() throws Exception {
        User user = createUser(true);
        String accessToken = tokenProvider.generateAccessToken(user.getId(), user.getRole());

        mockMvc.perform(post("/api/auth/refresh").contentType("application/json")
                .content(refreshBody(accessToken)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void deactivatedUserCannotRefreshToken() throws Exception {
        User user = createUser(false);
        String refreshToken = tokenProvider.generateRefreshToken(user.getId(), user.getRole());

        mockMvc.perform(post("/api/auth/refresh").contentType("application/json")
                .content(refreshBody(refreshToken)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    private User createUser(boolean active) {
        User user = new User();
        user.setFullName("Refresh User");
        user.setEmail("refresh-" + UUID.randomUUID() + "@example.com");
        user.setPassword("not-used-for-refresh");
        user.setRole(Role.STUDENT);
        user.setActive(active);
        return userRepository.saveAndFlush(user);
    }

    private String refreshBody(String refreshToken) {
        return """
                {"refreshToken":"%s"}
                """.formatted(refreshToken);
    }
}
