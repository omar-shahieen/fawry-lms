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

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
class SignupIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void validSignupCreatesStudentAndReturnsTokensWithoutPassword() throws Exception {
        String email = "signup-" + java.util.UUID.randomUUID() + "@example.com";
        String body = """
                {"fullName":"Signup Student","email":"%s","password":"plain-password"}
                """.formatted(email);

        mockMvc.perform(post("/api/auth/signup").contentType("application/json").content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                .andExpect(jsonPath("$.user.email").value(email))
                .andExpect(jsonPath("$.user.role").value("STUDENT"))
                .andExpect(jsonPath("$.user.profilePictureUrl").value(org.hamcrest.Matchers.startsWith(
                        "https://api.dicebear.com/10.x/lorelei/svg?seed=")))
                .andExpect(jsonPath("$.user.password").doesNotExist());

        User user = userRepository.findByEmail(email).orElseThrow();
        assertThat(user.getRole()).isEqualTo(Role.STUDENT);
        assertThat(user.isActive()).isTrue();
        assertThat(user.getProfilePictureUrl()).isNotBlank();
        assertThat(user.getPassword()).isNotEqualTo("plain-password");
        assertThat(passwordEncoder.matches("plain-password", user.getPassword())).isTrue();
    }

    @Test
    void ignoresClientSuppliedRole() throws Exception {
        String email = "signup-role-" + java.util.UUID.randomUUID() + "@example.com";
        String body = """
                {"fullName":"Signup Student","email":"%s","password":"plain-password","role":"ADMIN"}
                """.formatted(email);

        mockMvc.perform(post("/api/auth/signup").contentType("application/json").content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.user.role").value("STUDENT"));
        assertThat(userRepository.findByEmail(email).orElseThrow().getRole()).isEqualTo(Role.STUDENT);
    }

    @Test
    void duplicateEmailReturnsConflict() throws Exception {
        String email = "signup-duplicate-" + java.util.UUID.randomUUID() + "@example.com";
        String body = """
                {"fullName":"Signup Student","email":"%s","password":"plain-password"}
                """.formatted(email);

        mockMvc.perform(post("/api/auth/signup").contentType("application/json").content(body))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/api/auth/signup").contentType("application/json").content(body))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    void invalidEmailAndBlankPasswordReturnBadRequest() throws Exception {
        String body = """
                {"fullName":"Signup Student","email":"not-an-email","password":" "}
                """;

        mockMvc.perform(post("/api/auth/signup").contentType("application/json").content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.fieldErrors.email").exists())
                .andExpect(jsonPath("$.fieldErrors.password").exists());
    }
}
