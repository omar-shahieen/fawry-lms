package com.fawry.lms.user;

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

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
class AdminUserManagementIntegrationTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private UserRepository userRepository;

  @Autowired
  private JwtTokenProvider tokenProvider;

  @Test
  void adminCanCreatePromoteAndDeactivateUser() throws Exception {
    User admin = saveUser(Role.ADMIN, "Admin");
    String email = "managed-" + UUID.randomUUID() + "@example.com";

    String response = mockMvc.perform(post("/api/users")
        .header("Authorization", bearerToken(admin))
        .contentType("application/json")
        .content("""
            {
              "fullName": "Managed User",
              "email": "%s",
              "password": "secret-password",
              "role": "STUDENT"
            }
            """.formatted(email)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.role").value("STUDENT"))
        .andExpect(jsonPath("$.profilePictureUrl").isNotEmpty())
        .andReturn()
        .getResponse()
        .getContentAsString();

    UUID managedId = UUID.fromString(response.replaceAll(".*\\\"id\\\":\\\"([^\\\"]+)\\\".*", "$1"));
    User managed = userRepository.findById(managedId).orElseThrow();
    assertNotEquals("secret-password", managed.getPassword());
    assertNotNull(managed.getProfilePictureUrl());

    mockMvc.perform(patch("/api/users/" + managedId)
        .header("Authorization", bearerToken(admin))
        .contentType("application/json")
        .content("{\"role\":\"INSTRUCTOR\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.role").value("INSTRUCTOR"));

    mockMvc.perform(patch("/api/users/" + managedId + "/deactivate")
        .header("Authorization", bearerToken(admin)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.isActive").value(false));

    mockMvc.perform(post("/api/auth/login")
        .contentType("application/json")
        .content("""
            {
              "email": "%s",
              "password": "secret-password"
            }
            """.formatted(email)))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void nonAdminCannotManageUsers() throws Exception {
    User student = saveUser(Role.STUDENT, "Student");

    mockMvc.perform(post("/api/users")
        .header("Authorization", bearerToken(student))
        .contentType("application/json")
        .content("""
            {
              "fullName": "Another User",
              "email": "another-%s@example.com",
              "password": "secret-password",
              "role": "STUDENT"
            }
            """.formatted(UUID.randomUUID())))
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
