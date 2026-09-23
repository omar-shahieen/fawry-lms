package com.fawry.lms.quiz;

import com.fawry.lms.course.CourseRepository;
import com.fawry.lms.course.entities.Course;
import com.fawry.lms.quiz.repositories.QuizRepository;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
class QuizCrudIntegrationTest {

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
    void studentsSeeOnlyPublishedQuizzes() throws Exception {
        User instructor = saveUser(Role.INSTRUCTOR, "Instructor");
        User student = saveUser(Role.STUDENT, "Student");
        Course course = saveCourse(instructor);
        saveQuiz(course, "Published", true);
        saveQuiz(course, "Draft", false);

        mockMvc.perform(post("/api/courses/" + course.getId() + "/enroll")
                .header("Authorization", bearerToken(student)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/courses/" + course.getId() + "/quizzes")
                .header("Authorization", bearerToken(student)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].published").value(true));

        mockMvc.perform(get("/api/courses/" + course.getId() + "/quizzes")
                .header("Authorization", bearerToken(instructor)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2));
    }

    @Test
    void otherInstructorCannotUpdateQuiz() throws Exception {
        User instructor = saveUser(Role.INSTRUCTOR, "Instructor");
        User other = saveUser(Role.INSTRUCTOR, "Other");
        Course course = saveCourse(instructor);
        var quiz = saveQuiz(course, "Quiz", false);

        mockMvc.perform(patch("/api/quizzes/" + quiz.getId())
                .header("Authorization", bearerToken(other))
                .contentType("application/json")
                .content("{\"published\":true}"))
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

    private com.fawry.lms.quiz.entities.Quiz saveQuiz(Course course, String title, boolean published) {
        var quiz = new com.fawry.lms.quiz.entities.Quiz();
        quiz.setCourse(course);
        quiz.setTitle(title);
        quiz.setDurationMinutes(30);
        quiz.setPublished(published);
        return quizRepository.saveAndFlush(quiz);
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
