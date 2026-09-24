package com.fawry.lms.dashboard;

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
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

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
class StudentDashboardIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private CourseRepository courseRepository;
    @Autowired private EnrollmentRepository enrollmentRepository;
    @Autowired private QuizRepository quizRepository;
    @Autowired private QuizAttemptRepository attemptRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private JwtTokenProvider tokenProvider;

    @Test
    void containsOnlyEnrolledCoursesAndTheAuthenticatedStudentsQuizStatuses() throws Exception {
        User instructor = saveUser(Role.INSTRUCTOR, "Instructor");
        User student = saveUser(Role.STUDENT, "Student");
        User otherStudent = saveUser(Role.STUDENT, "Other Student");
        Course firstCourse = saveCourse(instructor, "First Course");
        Course secondCourse = saveCourse(instructor, "Second Course");
        Course unregisteredCourse = saveCourse(instructor, "Unregistered Course");
        enroll(student, firstCourse);
        enroll(student, secondCourse);
        Quiz attemptedQuiz = saveQuiz(firstCourse, "Attempted Quiz", true);
        Quiz unattemptedQuiz = saveQuiz(firstCourse, "Unattempted Quiz", true);
        Quiz anotherStudentsQuiz = saveQuiz(secondCourse, "Another Student's Quiz", true);
        saveQuiz(firstCourse, "Draft Quiz", false);
        saveQuiz(unregisteredCourse, "Unregistered Quiz", true);
        saveAttempt(attemptedQuiz, student, 2);
        saveAttempt(anotherStudentsQuiz, otherStudent, 4);

        var result = mockMvc.perform(get("/api/students/me/dashboard")
                        .header("Authorization", bearerToken(student)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.progress").doesNotExist())
                .andExpect(jsonPath("$.announcements").doesNotExist())
                .andReturn();

        List<Map<String, Object>> courses = JsonPath.read(result.getResponse().getContentAsString(), "$[*]");
        assertThat(courses).hasSize(2);
        assertThat(courses).extracting(course -> ((Number) course.get("courseId")).longValue())
                .containsExactlyInAnyOrder(firstCourse.getId(), secondCourse.getId());

        Map<String, Object> firstCourseResponse = courses.stream()
                .filter(course -> ((Number) course.get("courseId")).longValue() == firstCourse.getId())
                .findFirst().orElseThrow();
        List<Map<String, Object>> firstCourseQuizzes = (List<Map<String, Object>>) firstCourseResponse.get("quizzes");
        assertThat(firstCourseQuizzes).hasSize(2);
        assertQuizStatus(firstCourseQuizzes, attemptedQuiz.getId(), true, 2);
        assertQuizStatus(firstCourseQuizzes, unattemptedQuiz.getId(), false, null);

        Map<String, Object> secondCourseResponse = courses.stream()
                .filter(course -> ((Number) course.get("courseId")).longValue() == secondCourse.getId())
                .findFirst().orElseThrow();
        List<Map<String, Object>> secondCourseQuizzes = (List<Map<String, Object>>) secondCourseResponse.get("quizzes");
        assertQuizStatus(secondCourseQuizzes, anotherStudentsQuiz.getId(), false, null);

        mockMvc.perform(get("/api/students/me/dashboard")
                        .header("Authorization", bearerToken(instructor)))
                .andExpect(status().isForbidden());
    }

    private void assertQuizStatus(List<Map<String, Object>> quizzes, Long quizId, boolean attempted, Integer score) {
        Map<String, Object> quiz = quizzes.stream()
                .filter(value -> ((Number) value.get("quizId")).longValue() == quizId)
                .findFirst().orElseThrow();
        assertThat(quiz.get("attempted")).isEqualTo(attempted);
        assertThat(quiz.get("score")).isEqualTo(score);
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

    private Quiz saveQuiz(Course course, String title, boolean published) {
        Quiz quiz = new Quiz();
        quiz.setCourse(course);
        quiz.setTitle(title);
        quiz.setDurationMinutes(30);
        quiz.setPublished(published);
        return quizRepository.saveAndFlush(quiz);
    }

    private void saveAttempt(Quiz quiz, User student, int score) {
        QuizAttempt attempt = new QuizAttempt();
        attempt.setQuiz(quiz);
        attempt.setStudent(student);
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
