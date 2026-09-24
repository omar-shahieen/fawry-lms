package com.fawry.lms.dashboard;

import com.fawry.lms.communication.AnnouncementRepository;
import com.fawry.lms.communication.entities.Announcement;
import com.fawry.lms.course.CourseRepository;
import com.fawry.lms.course.entities.Course;
import com.fawry.lms.quiz.entities.Quiz;
import com.fawry.lms.quiz.entities.QuizAttempt;
import com.fawry.lms.quiz.repositories.QuizAttemptRepository;
import com.fawry.lms.quiz.repositories.QuizRepository;
import com.fawry.lms.security.JwtTokenProvider;
import com.fawry.lms.user.entities.Role;
import com.fawry.lms.user.entities.User;
import com.fawry.lms.user.repositories.UserRepository;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
class InstructorDashboardIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private CourseRepository courseRepository;
    @Autowired private QuizRepository quizRepository;
    @Autowired private QuizAttemptRepository attemptRepository;
    @Autowired private AnnouncementRepository announcementRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private JwtTokenProvider tokenProvider;

    @Test
    void includesOnlyOwnedCoursesTheirSubmittedQuizSummariesAndTheirAnnouncements() throws Exception {
        User instructor = saveUser(Role.INSTRUCTOR, "Instructor");
        User otherInstructor = saveUser(Role.INSTRUCTOR, "Other Instructor");
        User admin = saveUser(Role.ADMIN, "Admin");
        User studentOne = saveUser(Role.STUDENT, "Student One");
        User studentTwo = saveUser(Role.STUDENT, "Student Two");
        Course ownedCourse = saveCourse(instructor, "Owned Course");
        Course emptyOwnedCourse = saveCourse(instructor, "Empty Owned Course");
        Course otherCourse = saveCourse(otherInstructor, "Other Course");
        Quiz ownedQuiz = saveQuiz(ownedCourse);
        saveAttempt(ownedQuiz, studentOne, 2);
        saveAttempt(ownedQuiz, studentTwo, 4);
        saveAttempt(saveQuiz(otherCourse), studentTwo, 5);
        saveAnnouncement(ownedCourse, instructor, "Instructor notice");
        saveAnnouncement(ownedCourse, admin, "Admin notice");

        var result = mockMvc.perform(get("/api/instructors/me/dashboard")
                        .header("Authorization", bearerToken(instructor)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sections").doesNotExist())
                .andExpect(jsonPath("$.discussion").doesNotExist())
                .andReturn();

        Map<String, Object> payload = JsonPath.read(result.getResponse().getContentAsString(), "$");
        List<Map<String, Object>> courses = (List<Map<String, Object>>) payload.get("courses");
        assertThat(courses).hasSize(2);
        Map<String, Object> summary = courses.stream()
                .filter(course -> ((Number) course.get("courseId")).longValue() == ownedCourse.getId())
                .findFirst().orElseThrow();
        assertThat(((Number) summary.get("submittedAttemptCount")).longValue()).isEqualTo(2);
        assertThat(((Number) summary.get("averageScore")).doubleValue()).isEqualTo(3.0);
        Map<String, Object> emptySummary = courses.stream()
                .filter(course -> ((Number) course.get("courseId")).longValue() == emptyOwnedCourse.getId())
                .findFirst().orElseThrow();
        assertThat(((Number) emptySummary.get("submittedAttemptCount")).longValue()).isZero();
        assertThat(emptySummary.get("averageScore")).isNull();

        List<Map<String, Object>> announcements = (List<Map<String, Object>>) payload.get("announcements");
        assertThat(announcements).hasSize(1);
        assertThat(announcements.get(0).get("title")).isEqualTo("Instructor notice");

        mockMvc.perform(get("/api/instructors/me/dashboard")
                        .header("Authorization", bearerToken(admin)))
                .andExpect(status().isForbidden());
    }

    private Course saveCourse(User instructor, String title) {
        Course course = new Course();
        course.setTitle(title);
        course.setCode("COURSE-" + UUID.randomUUID());
        course.setTerm("Fall 2026");
        course.setInstructor(instructor);
        return courseRepository.saveAndFlush(course);
    }

    private Quiz saveQuiz(Course course) {
        Quiz quiz = new Quiz();
        quiz.setCourse(course);
        quiz.setTitle("Instructor Quiz");
        quiz.setDurationMinutes(30);
        quiz.setPublished(true);
        return quizRepository.saveAndFlush(quiz);
    }

    private void saveAttempt(Quiz quiz, User student, int score) {
        QuizAttempt attempt = new QuizAttempt();
        attempt.setQuiz(quiz);
        attempt.setStudent(student);
        attempt.setScore(score);
        attempt.setTotalQuestions(5);
        attempt.setSubmittedAt(Instant.now());
        attemptRepository.saveAndFlush(attempt);
    }

    private void saveAnnouncement(Course course, User author, String title) {
        Announcement announcement = new Announcement();
        announcement.setCourse(course);
        announcement.setAuthor(author);
        announcement.setTitle(title);
        announcement.setBody("Announcement body");
        announcementRepository.saveAndFlush(announcement);
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
