package com.fawry.lms.common;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import com.fawry.lms.security.JwtTokenProvider;
import com.fawry.lms.user.Role;
import com.fawry.lms.user.User;
import com.fawry.lms.user.UserRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Import(GlobalExceptionHandlerIntegrationTest.ThrowingController.class)
class GlobalExceptionHandlerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtTokenProvider tokenProvider;

    @Autowired
    private UserRepository userRepository;

    @Test
    void unexpectedExceptionReturnsStandardErrorResponseWithoutInternalDetails() throws Exception {
        mockMvc.perform(get("/test/errors/unexpected").header("Authorization", bearerToken()))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.error").value("Internal Server Error"))
                .andExpect(jsonPath("$.message").value("An unexpected error occurred."))
                .andExpect(jsonPath("$.message", not(containsString("sensitive test detail"))));
    }

    @Test
    void unknownApiPathReturnsStandardNotFoundResponse() throws Exception {
        mockMvc.perform(get("/api/route-that-does-not-exist").header("Authorization", bearerToken()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("The requested endpoint was not found."));
    }

    private String bearerToken() {
        User user = new User();
        user.setFullName("Error Test User");
        user.setEmail("error-test-" + java.util.UUID.randomUUID() + "@example.com");
        user.setPassword("hashed-test-password");
        user.setRole(Role.ADMIN);
        user = userRepository.saveAndFlush(user);
        return "Bearer " + tokenProvider.generateAccessToken(user.getId(), user.getRole());
    }

    @RestController
    static class ThrowingController {
        @GetMapping("/test/errors/unexpected")
        String throwUnexpectedException() {
            throw new IllegalStateException("sensitive test detail");
        }
    }
}
