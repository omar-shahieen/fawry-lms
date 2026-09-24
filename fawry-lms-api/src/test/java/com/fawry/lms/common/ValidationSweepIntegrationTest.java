package com.fawry.lms.common;

import com.fawry.lms.communication.DiscussionPostRepository;
import com.fawry.lms.communication.entities.DiscussionPost;
import com.fawry.lms.course.CourseRepository;
import com.fawry.lms.course.EnrollmentRepository;
import com.fawry.lms.course.entities.Course;
import com.fawry.lms.course.entities.Enrollment;
import com.fawry.lms.quiz.entities.Quiz;
import com.fawry.lms.quiz.repositories.QuizRepository;
import com.fawry.lms.security.JwtTokenProvider;
import com.fawry.lms.section.entities.Section;
import com.fawry.lms.section.repositories.SectionRepository;
import com.fawry.lms.section.entities.MarkdownContent;
import com.fawry.lms.section.repositories.MarkdownContentRepository;
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
import org.springframework.test.web.servlet.ResultActions;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
class ValidationSweepIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;
    @Autowired private CourseRepository courseRepository;
    @Autowired private EnrollmentRepository enrollmentRepository;
    @Autowired private SectionRepository sectionRepository;
    @Autowired private MarkdownContentRepository contentRepository;
    @Autowired private QuizRepository quizRepository;
    @Autowired private DiscussionPostRepository discussionRepository;
    @Autowired private JwtTokenProvider tokenProvider;

    @Test
    void rejectsBlankFieldsOnMutationsAndMalformedEnumAndForeignKeysAsClientErrors() throws Exception {
        User admin = saveUser(Role.ADMIN, "Admin");
        User student = saveUser(Role.STUDENT, "Student");
        Course course = saveCourse(saveUser(Role.INSTRUCTOR, "Instructor"));
        enroll(student, course);
        Section section = saveSection(course);
        MarkdownContent content = saveContent(section);
        Quiz quiz = saveQuiz(course);
        DiscussionPost post = savePost(course, student);

        assertFieldErrors(validationPost("/api/auth/signup", null,
                "{\"fullName\":\" \",\"email\":\"bad\",\"password\":\"\"}"));
        assertFieldErrors(validationPost("/api/auth/login", null,
                "{\"email\":\" \",\"password\":\"\"}"));
        assertFieldErrors(validationPost("/api/auth/refresh", null, "{\"refreshToken\":\" \"}"));
        assertFieldErrors(validationPatch("/api/users/me", student,
                "{\"fullName\":\" \"}"));
        assertFieldErrors(validationPost("/api/users", admin,
                "{\"fullName\":\" \",\"email\":\"admin@example.com\",\"password\":\"pw\",\"role\":\"ADMIN\"}"));
        assertFieldErrors(validationPatch("/api/users/" + student.getId(), admin,
                "{\"fullName\":\" \"}"));

        assertFieldErrors(validationPost("/api/courses", admin,
                "{\"title\":\" \",\"code\":\"CODE\",\"term\":\"Fall\",\"instructorId\":\"" + UUID.randomUUID() + "\"}"));
        assertFieldErrors(validationPatch("/api/courses/1", admin, "{\"title\":\" \"}"));
        assertFieldErrors(validationPatch("/api/courses/1/assign-instructor", admin, "{\"instructorId\":null}"));
        assertFieldErrors(validationPost("/api/courses/1/sections", admin, "{\"title\":\" \",\"orderIndex\":1}"));
        assertFieldErrors(validationPatch("/api/sections/1", admin, "{\"title\":\" \"}"));
        assertFieldErrors(validationPost("/api/sections/1/content", admin,
                "{\"title\":\"Title\",\"body\":\" \"}"));
        assertFieldErrors(validationPatch("/api/content/1", admin, "{\"body\":\" \"}"));

        assertFieldErrors(validationPost("/api/courses/1/quizzes", admin,
                "{\"title\":\" \",\"durationMinutes\":30,\"published\":false}"));
        assertFieldErrors(validationPost("/api/courses/1/quizzes", admin,
                "{\"title\":\"Valid title\",\"durationMinutes\":30}"));
        assertFieldErrors(validationPatch("/api/quizzes/" + quiz.getId(), admin, "{\"durationMinutes\":0}"));
        assertFieldErrors(validationPost("/api/quizzes/" + quiz.getId() + "/questions", admin,
                "{\"text\":\" \",\"orderIndex\":1,\"options\":[{\"text\":\" \",\"isCorrect\":true}]}"));
        assertFieldErrors(validationPatch("/api/questions/1", admin, "{\"text\":\" \"}"));
        assertFieldErrors(validationPost("/api/quizzes/" + quiz.getId() + "/submit", student,
                "{\"answers\":[{\"questionId\":null}]}"));

        assertFieldErrors(validationPost("/api/courses/1/discussion", admin,
                "{\"title\":\" \",\"body\":\"Body\"}"));
        assertFieldErrors(validationPost("/api/discussion/" + post.getId() + "/reply", admin,
                "{\"body\":\" \"}"));
        assertFieldErrors(validationPatch("/api/discussion/" + post.getId(), student,
                "{\"body\":\" \"}"));
        assertFieldErrors(validationPost("/api/courses/1/announcements", admin,
                "{\"title\":\" \",\"body\":\"Body\"}"));
        assertFieldErrors(validationPatch("/api/announcements/1", admin,
                "{\"title\":\"Title\",\"body\":\" \"}"));

        mockMvc.perform(post("/api/users")
                        .header("Authorization", bearerToken(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fullName\":\"User\",\"email\":\"user@example.com\",\"password\":\"pw\",\"role\":\"NOT_A_ROLE\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("The request body contains invalid or malformed values."));

        mockMvc.perform(post("/api/courses")
                        .header("Authorization", bearerToken(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Course\",\"code\":\"CODE-" + UUID.randomUUID()
                                + "\",\"term\":\"Fall\",\"instructorId\":\"" + UUID.randomUUID() + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Course instructor must have INSTRUCTOR role."));
    }

    private void assertFieldErrors(ResultActions action) throws Exception {
        action.andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors").isNotEmpty());
    }

    private ResultActions validationPost(String path, User user, String body) throws Exception {
        var builder = post(path).contentType(MediaType.APPLICATION_JSON).content(body);
        if (user != null) builder.header("Authorization", bearerToken(user));
        return mockMvc.perform(builder);
    }

    private ResultActions validationPatch(String path, User user, String body) throws Exception {
        return mockMvc.perform(patch(path)
                .header("Authorization", bearerToken(user))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }

    private void enroll(User student, Course course) {
        Enrollment enrollment = new Enrollment();
        enrollment.setStudent(student);
        enrollment.setCourse(course);
        enrollmentRepository.saveAndFlush(enrollment);
    }

    private Course saveCourse(User instructor) {
        Course course = new Course();
        course.setTitle("Validation Course");
        course.setCode("COURSE-" + UUID.randomUUID());
        course.setTerm("Fall 2026");
        course.setInstructor(instructor);
        return courseRepository.saveAndFlush(course);
    }

    private Section saveSection(Course course) {
        Section section = new Section();
        section.setCourse(course);
        section.setTitle("Section");
        section.setOrderIndex(1);
        return sectionRepository.saveAndFlush(section);
    }

    private MarkdownContent saveContent(Section section) {
        MarkdownContent content = new MarkdownContent();
        content.setSection(section);
        content.setTitle("Content");
        content.setBody("Body");
        return contentRepository.saveAndFlush(content);
    }

    private Quiz saveQuiz(Course course) {
        Quiz quiz = new Quiz();
        quiz.setCourse(course);
        quiz.setTitle("Quiz");
        quiz.setDurationMinutes(30);
        quiz.setPublished(true);
        return quizRepository.saveAndFlush(quiz);
    }

    private DiscussionPost savePost(Course course, User author) {
        DiscussionPost post = new DiscussionPost();
        post.setCourse(course);
        post.setAuthor(author);
        post.setTitle("Post");
        post.setBody("Body");
        return discussionRepository.saveAndFlush(post);
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
