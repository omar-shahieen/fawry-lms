package com.fawry.lms.grade;

import com.fawry.lms.course.CourseRepository;
import com.fawry.lms.course.EnrollmentRepository;
import com.fawry.lms.course.entities.Course;
import com.fawry.lms.course.entities.Enrollment;
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
class StudentGradesIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private CourseRepository courseRepository;
    @Autowired private EnrollmentRepository enrollmentRepository;
    @Autowired private QuizRepository quizRepository;
    @Autowired private QuizAttemptRepository attemptRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private JwtTokenProvider tokenProvider;

    @Test
    void groupsSubmittedResultsByTheAuthenticatedStudentsEnrolledCourses() throws Exception {
        User instructor = saveUser(Role.INSTRUCTOR, "Instructor");
        User firstStudent = saveUser(Role.STUDENT, "First Student");
        User secondStudent = saveUser(Role.STUDENT, "Second Student");
        Course firstCourse = saveCourse(instructor, "First Course");
        Course secondCourse = saveCourse(instructor, "Second Course");
        enroll(firstStudent, firstCourse);
        enroll(firstStudent, secondCourse);
        enroll(secondStudent, firstCourse);
        Quiz firstQuiz = saveQuiz(firstCourse, "First Quiz");
        Quiz secondQuiz = saveQuiz(firstCourse, "Second Student Quiz");
        saveAttempt(firstQuiz, firstStudent, 3);
        saveAttempt(secondQuiz, secondStudent, 4);

        mockMvc.perform(get("/api/students/me/grades")
                        .header("Authorization", bearerToken(firstStudent)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[?(@.courseId == " + firstCourse.getId() + ")].quizzes[0].quizId")
                        .value(firstQuiz.getId().intValue()))
                .andExpect(jsonPath("$[?(@.courseId == " + firstCourse.getId() + ")].quizzes[0].score")
                        .value(3))
                .andExpect(jsonPath("$[?(@.courseId == " + secondCourse.getId() + ")].quizzes.length()")
                        .value(0));

        mockMvc.perform(get("/api/students/me/grades")
                        .header("Authorization", bearerToken(secondStudent)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].quizzes[0].quizId").value(secondQuiz.getId().intValue()))
                .andExpect(jsonPath("$[0].quizzes[0].score").value(4));

        mockMvc.perform(get("/api/students/me/grades")
                        .header("Authorization", bearerToken(instructor)))
                .andExpect(status().isForbidden());
    }

    private void enroll(User student, Course course) {
        Enrollment enrollment = new Enrollment();
        enrollment.setStudent(student);
        enrollment.setCourse(course);
        enrollmentRepository.saveAndFlush(enrollment);
    }

    private Course saveCourse(User instructor, String title) {
        Course course = new Course();
        course.setTitle(title);
        course.setCode("COURSE-" + UUID.randomUUID());
        course.setTerm("Fall 2026");
        course.setInstructor(instructor);
        return courseRepository.saveAndFlush(course);
    }

    private Quiz saveQuiz(Course course, String title) {
        Quiz quiz = new Quiz();
        quiz.setCourse(course);
        quiz.setTitle(title);
        quiz.setDurationMinutes(30);
        quiz.setPublished(true);
        return quizRepository.saveAndFlush(quiz);
    }

    private void saveAttempt(Quiz quiz, User student, int score) {
        QuizAttempt attempt = new QuizAttempt();
        attempt.setQuiz(quiz);
        attempt.setStudent(student);
        attempt.setSubmittedAt(Instant.now());
        attempt.setScore(score);
        attempt.setTotalQuestions(5);
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
