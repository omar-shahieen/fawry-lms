package com.fawry.lms.quiz;

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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
class QuestionIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private QuizRepository quizRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtTokenProvider tokenProvider;

    @Test
    void questionRequiresAtLeastTwoOptionsAndOneCorrectOption() throws Exception {
        User instructor = saveUser(Role.INSTRUCTOR, "Instructor");
        Course course = saveCourse(instructor);
        var quiz = saveQuiz(course);
        String invalid = """
                {
                  "text": "Question",
                  "orderIndex": 1,
                  "options": [{"text":"Only","isCorrect":true}]
                }
                """;

        mockMvc.perform(post("/api/quizzes/" + quiz.getId() + "/questions")
                        .header("Authorization", bearerToken(instructor))
                        .contentType("application/json")
                        .content(invalid))
                .andExpect(status().isBadRequest());
    }

    @Test
    void staffCanCreateAndReadQuestionCorrectness() throws Exception {
        User instructor = saveUser(Role.INSTRUCTOR, "Instructor");
        Course course = saveCourse(instructor);
        var quiz = saveQuiz(course);
        String valid = """
                {
                  "text": "Question",
                  "orderIndex": 1,
                  "options": [
                    {"text":"Wrong","isCorrect":false},
                    {"text":"Right","isCorrect":true}
                  ]
                }
                """;

        mockMvc.perform(post("/api/quizzes/" + quiz.getId() + "/questions")
                        .header("Authorization", bearerToken(instructor))
                        .contentType("application/json")
                        .content(valid))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.options[1].isCorrect").value(true));

        mockMvc.perform(get("/api/quizzes/" + quiz.getId() + "/questions")
                        .header("Authorization", bearerToken(instructor)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].options[0].isCorrect").value(false));
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
