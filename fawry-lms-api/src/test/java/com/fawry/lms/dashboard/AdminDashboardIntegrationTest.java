package com.fawry.lms.dashboard;

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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.EnumMap;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
class AdminDashboardIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;
    @Autowired private CourseRepository courseRepository;
    @Autowired private EnrollmentRepository enrollmentRepository;
    @Autowired private JwtTokenProvider tokenProvider;

    @Test
    void reportsExactRoleCourseAndEnrollmentCountsAndIsAdminOnly() throws Exception {
        EnumMap<Role, Long> before = new EnumMap<>(Role.class);
        for (Role role : Role.values()) {
            before.put(role, userRepository.countByRole(role));
        }
        long courseCountBefore = courseRepository.count();
        long enrollmentCountBefore = enrollmentRepository.count();

        User student = saveUser(Role.STUDENT, "Dashboard Student");
        User instructor = saveUser(Role.INSTRUCTOR, "Dashboard Instructor");
        User admin = saveUser(Role.ADMIN, "Dashboard Admin");
        Course course = saveCourse(instructor);
        enroll(student, course);

        mockMvc.perform(get("/api/admin/dashboard")
                        .header("Authorization", bearerToken(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userCountsByRole.STUDENT").value(before.get(Role.STUDENT) + 1))
                .andExpect(jsonPath("$.userCountsByRole.INSTRUCTOR").value(before.get(Role.INSTRUCTOR) + 1))
                .andExpect(jsonPath("$.userCountsByRole.ADMIN").value(before.get(Role.ADMIN) + 1))
                .andExpect(jsonPath("$.totalCourseCount").value(courseCountBefore + 1))
                .andExpect(jsonPath("$.totalEnrollmentCount").value(enrollmentCountBefore + 1));

        mockMvc.perform(get("/api/admin/dashboard")
                        .header("Authorization", bearerToken(student)))
                .andExpect(status().isForbidden());
    }

    private User saveUser(Role role, String name) {
        User user = new User();
        user.setFullName(name);
        user.setEmail(name.toLowerCase().replace(' ', '-') + "-" + UUID.randomUUID() + "@example.com");
        user.setPassword("hashed-test-password");
        user.setRole(role);
        return userRepository.saveAndFlush(user);
    }

    private Course saveCourse(User instructor) {
        Course course = new Course();
        course.setTitle("Dashboard Course");
        course.setCode("COURSE-" + UUID.randomUUID());
        course.setTerm("Fall 2026");
        course.setInstructor(instructor);
        return courseRepository.saveAndFlush(course);
    }

    private void enroll(User student, Course course) {
        Enrollment enrollment = new Enrollment();
        enrollment.setStudent(student);
        enrollment.setCourse(course);
        enrollmentRepository.saveAndFlush(enrollment);
    }

    private String bearerToken(User user) {
        String token = tokenProvider.generateAccessToken(user.getId(), user.getRole());
        user.setAccessToken(token);
        userRepository.saveAndFlush(user);
        return "Bearer " + token;
    }
}
