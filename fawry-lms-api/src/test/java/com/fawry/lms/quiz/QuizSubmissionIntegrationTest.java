package com.fawry.lms.quiz;

import com.fawry.lms.course.CourseRepository;
import com.fawry.lms.course.entities.Course;
import com.fawry.lms.quiz.entities.Question;
import com.fawry.lms.quiz.entities.QuestionOption;
import com.fawry.lms.quiz.repositories.QuestionRepository;
import com.fawry.lms.quiz.repositories.QuizAttemptRepository;
import com.fawry.lms.quiz.repositories.QuizRepository;
import com.fawry.lms.security.JwtTokenProvider;
import com.fawry.lms.user.repositories.UserRepository;
import com.fawry.lms.user.entities.Role;
import com.fawry.lms.user.entities.User;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
class QuizSubmissionIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private QuizRepository quizRepository;

    @Autowired
    private QuestionRepository questionRepository;

    @Autowired
    private QuizAttemptRepository attemptRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtTokenProvider tokenProvider;

    @Test
    void submissionAutogradesAndSecondSubmissionConflicts() throws Exception {
        User instructor = saveUser(Role.INSTRUCTOR, "Instructor");
        User student = saveUser(Role.STUDENT, "Student");
        Course course = saveCourse(instructor);
        var quiz = saveQuiz(course);
        Question question = saveQuestion(quiz);
        QuestionOption wrong = question.getOptions().get(0);
        QuestionOption correct = question.getOptions().get(1);

        enroll(course, student);
        startAttempt(quiz, student);

        mockMvc.perform(post("/api/quizzes/" + quiz.getId() + "/submit")
                        .header("Authorization", bearerToken(student))
                        .contentType("application/json")
                        .content("{\"answers\":[{\"questionId\":" + question.getId()
                                + ",\"selectedOptionId\":" + correct.getId() + "}]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.score").value(1))
                .andExpect(jsonPath("$.totalQuestions").value(1))
                .andExpect(jsonPath("$.answers[0].isCorrect").value(true));

        mockMvc.perform(post("/api/quizzes/" + quiz.getId() + "/submit")
                        .header("Authorization", bearerToken(student))
                        .contentType("application/json")
                        .content("{\"answers\":[{\"questionId\":" + question.getId()
                                + ",\"selectedOptionId\":" + wrong.getId() + "}]}"))
                .andExpect(status().isConflict());
    }

    @Test
    void expiredAttemptIsRejectedUsingServerTime() throws Exception {
        User instructor = saveUser(Role.INSTRUCTOR, "Instructor");
        User student = saveUser(Role.STUDENT, "Student");
        Course course = saveCourse(instructor);
        var quiz = saveQuiz(course);
        Question question = saveQuestion(quiz);
        QuestionOption correct = question.getOptions().get(1);
        enroll(course, student);
        startAttempt(quiz, student);

        var attempt = attemptRepository.findByQuizIdAndStudentId(quiz.getId(), student.getId()).orElseThrow();
        attempt.setStartedAt(Instant.now().minusSeconds(3600));
        attemptRepository.saveAndFlush(attempt);

        mockMvc.perform(post("/api/quizzes/" + quiz.getId() + "/submit")
                        .header("Authorization", bearerToken(student))
                        .contentType("application/json")
                        .content("{\"answers\":[{\"questionId\":" + question.getId()
                                + ",\"selectedOptionId\":" + correct.getId() + "}]}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void concurrentSubmissionsProduceOneWinnerAndOneConflict() throws Exception {
        User instructor = saveUser(Role.INSTRUCTOR, "Instructor");
        User student = saveUser(Role.STUDENT, "Student");
        Course course = saveCourse(instructor);
        var quiz = saveQuiz(course);
        Question question = saveQuestion(quiz);
        QuestionOption correct = question.getOptions().get(1);
        enroll(course, student);
        startAttempt(quiz, student);
        String token = bearerToken(student);
        String body = "{\"answers\":[{\"questionId\":" + question.getId()
                + ",\"selectedOptionId\":" + correct.getId() + "}]}";

        CompletableFuture<Integer> first = CompletableFuture.supplyAsync(() -> submitStatus(quiz.getId(), token, body));
        CompletableFuture<Integer> second = CompletableFuture.supplyAsync(() -> submitStatus(quiz.getId(), token, body));
        int firstStatus = first.get();
        int secondStatus = second.get();

        org.junit.jupiter.api.Assertions.assertTrue(
                (firstStatus == 200 && secondStatus == 409) || (firstStatus == 409 && secondStatus == 200));
        assertEquals(1, attemptRepository.findByQuizIdAndStudentId(quiz.getId(), student.getId())
                .orElseThrow().getScore());
    }

    private int submitStatus(Long quizId, String token, String body) {
        try {
            return mockMvc.perform(post("/api/quizzes/" + quizId + "/submit")
                            .header("Authorization", token)
                            .contentType("application/json")
                            .content(body))
                    .andReturn().getResponse().getStatus();
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }

    private void enroll(Course course, User student) throws Exception {
        mockMvc.perform(post("/api/courses/" + course.getId() + "/enroll")
                        .header("Authorization", bearerToken(student)))
                .andExpect(status().isOk());
    }

    private void startAttempt(com.fawry.lms.quiz.entities.Quiz quiz, User student) throws Exception {
        mockMvc.perform(get("/api/quizzes/" + quiz.getId())
                        .header("Authorization", bearerToken(student)))
                .andExpect(status().isOk());
    }

    private Question saveQuestion(com.fawry.lms.quiz.entities.Quiz quiz) {
        Question question = new Question();
        question.setQuiz(quiz);
        question.setText("Question");
        question.setOrderIndex(1);
        QuestionOption wrong = new QuestionOption();
        wrong.setText("Wrong");
        wrong.setCorrect(false);
        QuestionOption correct = new QuestionOption();
        correct.setText("Correct");
        correct.setCorrect(true);
        question.addOption(wrong);
        question.addOption(correct);
        return questionRepository.saveAndFlush(question);
    }

    private com.fawry.lms.quiz.entities.Quiz saveQuiz(Course course) {
        var quiz = new com.fawry.lms.quiz.entities.Quiz();
        quiz.setCourse(course);
        quiz.setTitle("Quiz");
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
