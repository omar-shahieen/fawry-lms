package com.fawry.lms.user;

import com.fawry.lms.security.JwtTokenProvider;
import com.fawry.lms.user.entities.Role;
import com.fawry.lms.user.entities.User;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
class AdminUserIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtTokenProvider tokenProvider;

    @Test
    void adminCanFilterUsersWithPaginationMetadata() throws Exception {
        User admin = saveUser(Role.ADMIN, "Admin");
        saveUser(Role.STUDENT, "Student");
        saveUser(Role.INSTRUCTOR, "Instructor");

        mockMvc.perform(get("/api/users")
                        .param("role", "STUDENT")
                        .param("page", "0")
                        .param("size", "1")
                        .header("Authorization", bearerToken(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].role").value("STUDENT"))
                .andExpect(jsonPath("$.totalElements").isNumber())
                .andExpect(jsonPath("$.totalPages").isNumber());
    }

    @Test
    void nonAdminCannotListUsers() throws Exception {
        User student = saveUser(Role.STUDENT, "Student");

        mockMvc.perform(get("/api/users")
                        .header("Authorization", bearerToken(student)))
                .andExpect(status().isForbidden());
    }

    private User saveUser(Role role, String name) {
        User user = new User();
        user.setFullName(name);
        user.setEmail(name.toLowerCase() + "-" + UUID.randomUUID() + "@example.com");
        user.setPassword("hashed-test-password");
        user.setRole(role);
        return userRepository.saveAndFlush(user);
    }

    private String bearerToken(User user) {
        String token = tokenProvider.generateAccessToken(user.getId(), user.getRole());
        user.setAccessToken(token);
        userRepository.saveAndFlush(user);
        return "Bearer " + token;
    }
}
