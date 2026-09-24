package com.fawry.lms.quiz;

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
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
class QuizAttemptHistoryIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private CourseRepository courseRepository;
    @Autowired private QuizRepository quizRepository;
    @Autowired private QuizAttemptRepository attemptRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private JwtTokenProvider tokenProvider;

    @Test
    void studentsCanOnlyReadTheirOwnAttempt() throws Exception {
        User instructor = saveUser(Role.INSTRUCTOR, "Instructor");
        User firstStudent = saveUser(Role.STUDENT, "First Student");
        User secondStudent = saveUser(Role.STUDENT, "Second Student");
        Quiz quiz = saveQuiz(saveCourse(instructor));
        saveAttempt(quiz, firstStudent, 1);
        saveAttempt(quiz, secondStudent, 2);

        mockMvc.perform(get("/api/quizzes/{id}/attempts/me", quiz.getId())
                        .header("Authorization", bearerToken(firstStudent)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.studentId").value(firstStudent.getId().toString()))
                .andExpect(jsonPath("$.score").value(1))
                .andExpect(jsonPath("$.studentEmail").value(firstStudent.getEmail()))
                .andExpect(jsonPath("$.password").doesNotExist());

        mockMvc.perform(get("/api/quizzes/{id}/attempts/me", quiz.getId())
                        .header("Authorization", bearerToken(secondStudent)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.studentId").value(secondStudent.getId().toString()))
                .andExpect(jsonPath("$.score").value(2));

        mockMvc.perform(get("/api/quizzes/{id}/attempts", quiz.getId())
                        .header("Authorization", bearerToken(firstStudent)))
                .andExpect(status().isForbidden());
    }

    @Test
    void staffAttemptListIsPaginatedAndRestrictedToCourseOwnerOrAdmin() throws Exception {
        User owner = saveUser(Role.INSTRUCTOR, "Owner");
        User otherInstructor = saveUser(Role.INSTRUCTOR, "Other Instructor");
        User admin = saveUser(Role.ADMIN, "Admin");
        User firstStudent = saveUser(Role.STUDENT, "First Student");
        User secondStudent = saveUser(Role.STUDENT, "Second Student");
        Quiz quiz = saveQuiz(saveCourse(owner));
        saveAttempt(quiz, firstStudent, 1);
        saveAttempt(quiz, secondStudent, 2);

        mockMvc.perform(get("/api/quizzes/{id}/attempts", quiz.getId())
                        .param("page", "0").param("size", "1")
                        .header("Authorization", bearerToken(owner)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.totalPages").value(2));

        mockMvc.perform(get("/api/quizzes/{id}/attempts", quiz.getId())
                        .header("Authorization", bearerToken(otherInstructor)))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/quizzes/{id}/attempts", quiz.getId())
                        .param("page", "0").param("size", "1")
                        .header("Authorization", bearerToken(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.totalElements").value(2));
    }

    private Quiz saveQuiz(Course course) {
        Quiz quiz = new Quiz();
        quiz.setCourse(course);
        quiz.setTitle("History Quiz");
        quiz.setDurationMinutes(30);
        quiz.setPublished(true);
        return quizRepository.saveAndFlush(quiz);
    }

    private Course saveCourse(User instructor) {
        Course course = new Course();
        course.setTitle("Course");
        course.setCode("COURSE-" + UUID.randomUUID());
        course.setTerm("Fall 2026");
        course.setInstructor(instructor);
        return courseRepository.saveAndFlush(course);
    }

    private void saveAttempt(Quiz quiz, User student, int score) {
        QuizAttempt attempt = new QuizAttempt();
        attempt.setQuiz(quiz);
        attempt.setStudent(student);
        attempt.setStartedAt(Instant.now());
        attempt.setSubmittedAt(Instant.now());
        attempt.setScore(score);
        attempt.setTotalQuestions(2);
        attemptRepository.saveAndFlush(attempt);
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
