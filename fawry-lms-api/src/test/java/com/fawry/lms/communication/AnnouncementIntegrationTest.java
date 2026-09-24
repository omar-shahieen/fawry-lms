package com.fawry.lms.communication;

import com.fawry.lms.course.CourseRepository;
import com.fawry.lms.course.EnrollmentRepository;
import com.fawry.lms.course.entities.Course;
import com.fawry.lms.course.entities.Enrollment;
import com.fawry.lms.security.JwtTokenProvider;
import com.fawry.lms.user.entities.Role;
import com.fawry.lms.user.entities.User;
import com.fawry.lms.user.repositories.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
class AnnouncementIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private CourseRepository courseRepository;
    @Autowired private EnrollmentRepository enrollmentRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private JwtTokenProvider tokenProvider;

    @Test
    void announcementsAreEnrollmentReadableAndOwnerAdminWritableNewestFirstAndNotFoundIs404() throws Exception {
        User owner = saveUser(Role.INSTRUCTOR, "Owner");
        User otherInstructor = saveUser(Role.INSTRUCTOR, "Other Instructor");
        User student = saveUser(Role.STUDENT, "Student");
        User unenrolled = saveUser(Role.STUDENT, "Unenrolled");
        User admin = saveUser(Role.ADMIN, "Admin");
        Course course = saveCourse(owner);
        enroll(student, course);

        MvcResult firstCreated = mockMvc.perform(post("/api/courses/{courseId}/announcements", course.getId())
                        .header("Authorization", bearerToken(owner))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"First\",\"body\":\"First body\"}"))
                .andExpect(status().isOk())
                .andReturn();
        MvcResult secondCreated = mockMvc.perform(post("/api/courses/{courseId}/announcements", course.getId())
                        .header("Authorization", bearerToken(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Second\",\"body\":\"Second body\"}"))
                .andExpect(status().isOk())
                .andReturn();
        Long firstId = idFrom(firstCreated);
        Long secondId = idFrom(secondCreated);

        mockMvc.perform(get("/api/courses/{courseId}/announcements", course.getId())
                        .header("Authorization", bearerToken(student)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[0].id").value(secondId.intValue()))
                .andExpect(jsonPath("$.content[1].id").value(firstId.intValue()));

        mockMvc.perform(get("/api/courses/{courseId}/announcements", course.getId())
                        .header("Authorization", bearerToken(unenrolled)))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/courses/{courseId}/announcements", course.getId())
                        .header("Authorization", bearerToken(student))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Denied\",\"body\":\"Denied\"}"))
                .andExpect(status().isForbidden());

        mockMvc.perform(patch("/api/announcements/{id}", firstId)
                        .header("Authorization", bearerToken(otherInstructor))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Updated\",\"body\":\"Updated body\"}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(patch("/api/announcements/{id}", firstId)
                        .header("Authorization", bearerToken(owner))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Updated\",\"body\":\"Updated body\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Updated"));

        mockMvc.perform(delete("/api/announcements/{id}", firstId)
                        .header("Authorization", bearerToken(admin)))
                .andExpect(status().isOk());
        mockMvc.perform(patch("/api/announcements/{id}", 99999999L)
                        .header("Authorization", bearerToken(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Missing\",\"body\":\"Missing\"}"))
                .andExpect(status().isNotFound());
    }

    private Long idFrom(MvcResult result) throws Exception {
        Number id = com.jayway.jsonpath.JsonPath.read(result.getResponse().getContentAsString(), "$.id");
        return id.longValue();
    }

    private void enroll(User student, Course course) {
        Enrollment enrollment = new Enrollment();
        enrollment.setStudent(student);
        enrollment.setCourse(course);
        enrollmentRepository.saveAndFlush(enrollment);
    }

    private Course saveCourse(User instructor) {
        Course course = new Course();
        course.setTitle("Announcement Course");
        course.setCode("COURSE-" + UUID.randomUUID());
        course.setTerm("Fall 2026");
        course.setInstructor(instructor);
        return courseRepository.saveAndFlush(course);
    }

    private User saveUser(Role role, String name) {
        User user = new User();
        user.setFullName(name);
        user.setEmail(name.toLowerCase().replace(' ', '-') + "-" + UUID.randomUUID() + "@example.com");
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
