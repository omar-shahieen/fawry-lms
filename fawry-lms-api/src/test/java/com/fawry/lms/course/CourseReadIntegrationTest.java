package com.fawry.lms.course;

import com.fawry.lms.course.entities.Course;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
class CourseReadIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtTokenProvider tokenProvider;

    @Test
    void listFiltersActiveCoursesAndGetByIdIncludesInactiveCourse() throws Exception {
        User instructor = saveUser("Instructor");
        Course active = saveCourse(instructor, "Spring Security", "CS-SEC", "Fall 2026", true);
        Course inactive = saveCourse(instructor, "Spring Data", "CS-DATA", "Fall 2026", false);

        mockMvc.perform(get("/api/courses")
                .param("search", "security")
                .param("term", "Fall 2026")
                .param("page", "0")
                .param("size", "1")
                .header("Authorization", bearerToken(instructor)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].id").value(active.getId()))
                .andExpect(jsonPath("$.content[0].isActive").value(true));

        mockMvc.perform(get("/api/courses/" + inactive.getId())
                .header("Authorization", bearerToken(instructor)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(inactive.getId()))
                .andExpect(jsonPath("$.isActive").value(false));
    }

    @Test
    void missingCourseReturnsNotFound() throws Exception {
        User instructor = saveUser("Instructor");

        mockMvc.perform(get("/api/courses/999999")
                .header("Authorization", bearerToken(instructor)))
                .andExpect(status().isNotFound());
    }

    @Test
    void listWithoutSearchTermOrCodeReturnsActiveCourses() throws Exception {
        User instructor = saveUser("Instructor");
        Course active = saveCourse(instructor, "Operating Systems", "CS-OS", "Term-Null-Search", true);
        saveCourse(instructor, "Hidden Course", "CS-HIDE", "Term-Null-Search", false);

        mockMvc.perform(get("/api/courses")
                .param("term", "Term-Null-Search")
                .param("page", "0")
                .param("size", "10")
                .header("Authorization", bearerToken(instructor)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].id").value(active.getId()));
    }

    private Course saveCourse(User instructor, String title, String code, String term, boolean active) {
        Course course = new Course();
        course.setTitle(title);
        course.setCode(code + "-" + UUID.randomUUID());
        course.setTerm(term);
        course.setInstructor(instructor);
        course.setActive(active);
        return courseRepository.saveAndFlush(course);
    }

    private User saveUser(String name) {
        User user = new User();
        user.setFullName(name);
        user.setEmail(name.toLowerCase() + "-" + UUID.randomUUID() + "@example.com");
        user.setPassword("hashed-test-password");
        user.setRole(Role.INSTRUCTOR);
        return userRepository.saveAndFlush(user);
    }

    private String bearerToken(User user) {
        String token = tokenProvider.generateAccessToken(user.getId(), user.getRole());
        user.setAccessToken(token);
        userRepository.saveAndFlush(user);
        return "Bearer " + token;
    }
}
