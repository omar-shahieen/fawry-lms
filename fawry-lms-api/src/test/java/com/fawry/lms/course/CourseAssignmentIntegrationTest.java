package com.fawry.lms.course;

import com.fawry.lms.course.entities.Course;
import com.fawry.lms.security.JwtTokenProvider;
import com.fawry.lms.user.UserRepository;
import com.fawry.lms.user.entities.Role;
import com.fawry.lms.user.entities.User;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
class CourseAssignmentIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtTokenProvider tokenProvider;

    @Test
    void adminCanAssignInstructor() throws Exception {
        User admin = saveUser(Role.ADMIN, "Admin");
        User first = saveUser(Role.INSTRUCTOR, "First");
        User second = saveUser(Role.INSTRUCTOR, "Second");
        Course course = saveCourse(first);

        mockMvc.perform(patch("/api/courses/" + course.getId() + "/assign-instructor")
                        .header("Authorization", bearerToken(admin))
                        .contentType("application/json")
                        .content("{\"instructorId\":\"" + second.getId() + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.instructorId").value(second.getId().toString()));
    }

    @Test
    void assigningNonInstructorReturnsBadRequest() throws Exception {
        User admin = saveUser(Role.ADMIN, "Admin");
        User instructor = saveUser(Role.INSTRUCTOR, "Instructor");
        User student = saveUser(Role.STUDENT, "Student");
        Course course = saveCourse(instructor);

        mockMvc.perform(patch("/api/courses/" + course.getId() + "/assign-instructor")
                        .header("Authorization", bearerToken(admin))
                        .contentType("application/json")
                        .content("{\"instructorId\":\"" + student.getId() + "\"}"))
                .andExpect(status().isBadRequest());
    }

    private Course saveCourse(User instructor) {
        Course course = new Course();
        course.setTitle("Course");
        course.setCode("COURSE-" + UUID.randomUUID());
        course.setTerm("Fall 2026");
        course.setInstructor(instructor);
        return courseRepository.saveAndFlush(course);
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
