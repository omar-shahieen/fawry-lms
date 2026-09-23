package com.fawry.lms.auth;

import com.fawry.lms.user.Role;
import com.fawry.lms.user.User;
import com.fawry.lms.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
class LoginIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void validCredentialsReturnTokensAndUserWithoutPassword() throws Exception {
        User user = createUser(true);
        String body = loginBody(user.getEmail(), "correct-password");

        mockMvc.perform(post("/api/auth/login").contentType("application/json").content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                .andExpect(jsonPath("$.user.id").value(user.getId().toString()))
                .andExpect(jsonPath("$.user.email").value(user.getEmail()))
                .andExpect(jsonPath("$.user.role").value("STUDENT"))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.user.password").doesNotExist());
    }

    @Test
    void wrongPasswordReturnsUnauthorized() throws Exception {
        User user = createUser(true);

        mockMvc.perform(post("/api/auth/login").contentType("application/json")
                        .content(loginBody(user.getEmail(), "wrong-password")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void inactiveUserCannotLoginWithCorrectPassword() throws Exception {
        User user = createUser(false);

        mockMvc.perform(post("/api/auth/login").contentType("application/json")
                        .content(loginBody(user.getEmail(), "correct-password")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    private User createUser(boolean active) {
        User user = new User();
        user.setFullName("Login User");
        user.setEmail("login-" + UUID.randomUUID() + "@example.com");
        user.setPassword(passwordEncoder.encode("correct-password"));
        user.setRole(Role.STUDENT);
        user.setActive(active);
        return userRepository.saveAndFlush(user);
    }

    private String loginBody(String email, String password) {
        return """
                {"email":"%s","password":"%s"}
                """.formatted(email, password);
    }
}
