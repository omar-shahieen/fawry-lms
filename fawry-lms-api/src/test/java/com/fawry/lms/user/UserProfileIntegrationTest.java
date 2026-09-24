package com.fawry.lms.user;

import com.fawry.lms.course.entities.Course;
import com.fawry.lms.course.entities.Enrollment;
import com.fawry.lms.course.EnrollmentRepository;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
class UserProfileIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EnrollmentRepository enrollmentRepository;

    @Autowired
    private com.fawry.lms.course.CourseRepository courseRepository;

    @Autowired
    private JwtTokenProvider tokenProvider;

    @Test
    void studentProfileIncludesEnrolledCourses() throws Exception {
        User instructor = saveUser(Role.INSTRUCTOR, "Instructor");
        User student = saveUser(Role.STUDENT, "Student");
        Course course = new Course();
        course.setTitle("Algorithms");
        course.setCode("ALG-" + UUID.randomUUID());
        course.setTerm("Fall 2026");
        course.setInstructor(instructor);
        course = courseRepository.saveAndFlush(course);

        Enrollment enrollment = new Enrollment();
        enrollment.setStudent(student);
        enrollment.setCourse(course);
        enrollmentRepository.saveAndFlush(enrollment);

        mockMvc.perform(get("/api/users/me").header("Authorization", bearerToken(student)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(student.getId().toString()))
                .andExpect(jsonPath("$.email").value(student.getEmail()))
                .andExpect(jsonPath("$.role").value("STUDENT"))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.accessToken").doesNotExist())
                .andExpect(jsonPath("$.enrolledCourses[0].id").value(course.getId()))
                .andExpect(jsonPath("$.enrolledCourses[0].title").value("Algorithms"));
    }

    @Test
    void instructorProfileDoesNotIncludeStudentEnrollmentData() throws Exception {
        User instructor = saveUser(Role.INSTRUCTOR, "Instructor");

        mockMvc.perform(get("/api/users/me").header("Authorization", bearerToken(instructor)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("INSTRUCTOR"))
                .andExpect(jsonPath("$.enrolledCourses").isEmpty());
    }

    @Test
    void profileRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/users/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void currentUserUpdateChangesOnlyAllowedFields() throws Exception {
        User student = saveUser(Role.STUDENT, "Student");

        mockMvc.perform(patch("/api/users/me")
                .header("Authorization", bearerToken(student))
                .contentType("application/json")
                .content("""
                        {
                          "fullName": "Updated Student",
                          "profilePictureUrl": "https://example.com/avatar.png",
                          "email": "changed@example.com",
                          "role": "ADMIN",
                          "isActive": false
                        }
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName").value("Updated Student"))
                .andExpect(jsonPath("$.profilePictureUrl").value("https://example.com/avatar.png"))
                .andExpect(jsonPath("$.email").value(student.getEmail()))
                .andExpect(jsonPath("$.role").value("STUDENT"));

        User reloaded = userRepository.findById(student.getId()).orElseThrow();
        org.junit.jupiter.api.Assertions.assertEquals(Role.STUDENT, reloaded.getRole());
        org.junit.jupiter.api.Assertions.assertTrue(reloaded.isActive());
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
