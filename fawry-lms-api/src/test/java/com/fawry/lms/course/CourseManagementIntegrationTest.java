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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
class CourseManagementIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtTokenProvider tokenProvider;

    @Test
    void instructorsCanUpdateOwnCourseButNotAnotherCourse() throws Exception {
        User instructor = saveUser(Role.INSTRUCTOR, "Instructor");
        User otherInstructor = saveUser(Role.INSTRUCTOR, "Other");
        Course course = saveCourse(instructor, "Original", "COURSE-" + UUID.randomUUID());

        mockMvc.perform(patch("/api/courses/" + course.getId())
                        .header("Authorization", bearerToken(instructor))
                        .contentType("application/json")
                        .content("{\"title\":\"Updated\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Updated"));

        mockMvc.perform(patch("/api/courses/" + course.getId())
                        .header("Authorization", bearerToken(otherInstructor))
                        .contentType("application/json")
                        .content("{\"title\":\"Rejected\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void instructorsCannotCreateAndAdminCanDeleteSoftly() throws Exception {
        User instructor = saveUser(Role.INSTRUCTOR, "Instructor");
        User admin = saveUser(Role.ADMIN, "Admin");
        String code = "COURSE-" + UUID.randomUUID();
        String body = """
                {
                  "title": "New Course",
                  "code": "%s",
                  "term": "Fall 2026",
                  "instructorId": "%s"
                }
                """.formatted(code, instructor.getId());

        mockMvc.perform(post("/api/courses")
                        .header("Authorization", bearerToken(instructor))
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isForbidden());

        String response = mockMvc.perform(post("/api/courses")
                        .header("Authorization", bearerToken(admin))
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String id = response.replaceAll(".*\\\"id\\\":([0-9]+).*", "$1");

        mockMvc.perform(delete("/api/courses/" + id)
                        .header("Authorization", bearerToken(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isActive").value(false));
    }

    @Test
    void duplicateCourseCodeReturnsConflict() throws Exception {
        User instructor = saveUser(Role.INSTRUCTOR, "Instructor");
        User admin = saveUser(Role.ADMIN, "Admin");
        String code = "DUPLICATE-" + UUID.randomUUID();
        String body = """
                {
                  "title": "Course",
                  "code": "%s",
                  "term": "Fall 2026",
                  "instructorId": "%s"
                }
                """.formatted(code, instructor.getId());

        mockMvc.perform(post("/api/courses")
                        .header("Authorization", bearerToken(admin))
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/courses")
                        .header("Authorization", bearerToken(admin))
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isConflict());
    }

    private Course saveCourse(User instructor, String title, String code) {
        Course course = new Course();
        course.setTitle(title);
        course.setCode(code);
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
