package com.fawry.lms.section;

import com.fawry.lms.course.CourseRepository;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
class SectionIntegrationTest {

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
    void enrolledStudentCanReadAndSectionsRemainOrdered() throws Exception {
        User instructor = saveUser(Role.INSTRUCTOR, "Instructor");
        User student = saveUser(Role.STUDENT, "Student");
        Course course = saveCourse(instructor);
        String enrollment = "";
        mockMvc.perform(post("/api/courses/" + course.getId() + "/enroll")
                        .header("Authorization", bearerToken(student)))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/courses/" + course.getId() + "/sections")
                        .header("Authorization", bearerToken(instructor))
                        .contentType("application/json")
                        .content("{\"title\":\"Second\",\"orderIndex\":2}"))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/courses/" + course.getId() + "/sections")
                        .header("Authorization", bearerToken(instructor))
                        .contentType("application/json")
                        .content("{\"title\":\"First\",\"orderIndex\":1}"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/courses/" + course.getId() + "/sections")
                        .header("Authorization", bearerToken(student)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("First"))
                .andExpect(jsonPath("$[1].title").value("Second"));
    }

    @Test
    void unenrolledStudentAndOtherInstructorAreDenied() throws Exception {
        User instructor = saveUser(Role.INSTRUCTOR, "Instructor");
        User other = saveUser(Role.INSTRUCTOR, "Other");
        User student = saveUser(Role.STUDENT, "Student");
        Course course = saveCourse(instructor);

        mockMvc.perform(get("/api/courses/" + course.getId() + "/sections")
                        .header("Authorization", bearerToken(student)))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/courses/" + course.getId() + "/sections")
                        .header("Authorization", bearerToken(other))
                        .contentType("application/json")
                        .content("{\"title\":\"Denied\",\"orderIndex\":1}"))
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
