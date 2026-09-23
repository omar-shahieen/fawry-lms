package com.fawry.lms.section;

import com.fawry.lms.course.CourseRepository;
import com.fawry.lms.course.entities.Course;
import com.fawry.lms.security.JwtTokenProvider;
import com.fawry.lms.section.entities.Section;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
class ContentIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private SectionRepository sectionRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtTokenProvider tokenProvider;

    @Test
    void enrolledStudentCanReadContentAndOtherStudentCannot() throws Exception {
        User instructor = saveUser(Role.INSTRUCTOR, "Instructor");
        User enrolled = saveUser(Role.STUDENT, "Enrolled");
        User other = saveUser(Role.STUDENT, "Other");
        Course course = saveCourse(instructor);
        Section section = saveSection(course);

        mockMvc.perform(post("/api/courses/" + course.getId() + "/enroll")
                        .header("Authorization", bearerToken(enrolled)))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/sections/" + section.getId() + "/content")
                        .header("Authorization", bearerToken(instructor))
                        .contentType("application/json")
                        .content("{\"title\":\"Intro\",\"body\":\"# Hello\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Intro"));

        mockMvc.perform(get("/api/sections/" + section.getId() + "/content")
                        .header("Authorization", bearerToken(enrolled)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].body").value("# Hello"));

        mockMvc.perform(get("/api/sections/" + section.getId() + "/content")
                        .header("Authorization", bearerToken(other)))
                .andExpect(status().isForbidden());
    }

    private Course saveCourse(User instructor) {
        Course course = new Course();
        course.setTitle("Course");
        course.setCode("COURSE-" + UUID.randomUUID());
        course.setTerm("Fall 2026");
        course.setInstructor(instructor);
        return courseRepository.saveAndFlush(course);
    }

    private Section saveSection(Course course) {
        Section section = new Section();
        section.setCourse(course);
        section.setTitle("Section");
        section.setOrderIndex(1);
        return sectionRepository.saveAndFlush(section);
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
