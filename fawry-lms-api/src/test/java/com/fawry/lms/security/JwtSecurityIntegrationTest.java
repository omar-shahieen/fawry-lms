package com.fawry.lms.security;

import com.fawry.lms.user.UserRepository;
import com.fawry.lms.user.entities.Role;
import com.fawry.lms.user.entities.User;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Import(JwtSecurityIntegrationTest.ProtectedController.class)
class JwtSecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtTokenProvider tokenProvider;

    @Autowired
    private UserRepository userRepository;

    @Test
    void rejectsRequestWithoutToken() throws Exception {
        mockMvc.perform(get("/test/security/protected"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"));
    }

    @Test
    void permitsUnauthenticatedHealthCheck() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    void acceptsValidAccessTokenAndMapsRoleToAuthority() throws Exception {
        User student = new User();
        student.setFullName("Security Test Student");
        student.setEmail("security-" + java.util.UUID.randomUUID() + "@example.com");
        student.setPassword("hashed-test-password");
        student.setRole(Role.STUDENT);
        student = userRepository.saveAndFlush(student);
        String token = tokenProvider.generateAccessToken(student.getId(), student.getRole());
        student.setAccessToken(token);
        userRepository.saveAndFlush(student);

        mockMvc.perform(get("/test/security/protected")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(content().string("ROLE_STUDENT"));
    }

    @RestController
    static class ProtectedController {
        @GetMapping("/test/security/protected")
        @PreAuthorize("isAuthenticated()")
        String protectedEndpoint() {
            return SecurityContextHolder.getContext().getAuthentication().getAuthorities()
                    .iterator().next().getAuthority();
        }
    }
}
