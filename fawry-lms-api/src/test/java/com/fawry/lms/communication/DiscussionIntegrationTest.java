package com.fawry.lms.communication;

import com.fawry.lms.communication.entities.DiscussionPost;
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

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
class DiscussionIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private CourseRepository courseRepository;
    @Autowired private EnrollmentRepository enrollmentRepository;
    @Autowired private DiscussionPostRepository discussionPostRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private JwtTokenProvider tokenProvider;

    @Test
    void enrolledStudentsInstructorAndAdminCanPostAndListNestedReplies() throws Exception {
        User instructor = saveUser(Role.INSTRUCTOR, "Instructor");
        User student = saveUser(Role.STUDENT, "Student");
        User admin = saveUser(Role.ADMIN, "Admin");
        User replyAuthor = saveUser(Role.STUDENT, "Reply Author");
        Course course = saveCourse(instructor);
        enroll(student, course);
        DiscussionPost parent = savePost(course, student, "Question", "Top-level body");
        DiscussionPost reply = new DiscussionPost();
        reply.setCourse(course);
        reply.setAuthor(replyAuthor);
        reply.setBody("Nested reply");
        reply.setParentPost(parent);
        discussionPostRepository.saveAndFlush(reply);

        mockMvc.perform(get("/api/courses/{courseId}/discussion", course.getId())
                        .header("Authorization", bearerToken(student)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].body").value("Top-level body"))
                .andExpect(jsonPath("$.content[0].replies.length()").value(1))
                .andExpect(jsonPath("$.content[0].replies[0].body").value("Nested reply"));

        mockMvc.perform(post("/api/courses/{courseId}/discussion", course.getId())
                        .header("Authorization", bearerToken(student))
                        .contentType("application/json")
                        .content("{\"title\":\"Student post\",\"body\":\"Student body\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.authorId").value(student.getId().toString()));

        mockMvc.perform(post("/api/courses/{courseId}/discussion", course.getId())
                        .header("Authorization", bearerToken(instructor))
                        .contentType("application/json")
                        .content("{\"title\":\"Instructor post\",\"body\":\"Instructor body\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.authorId").value(instructor.getId().toString()));

        mockMvc.perform(post("/api/courses/{courseId}/discussion", course.getId())
                        .header("Authorization", bearerToken(admin))
                        .contentType("application/json")
                        .content("{\"title\":\"Admin post\",\"body\":\"Admin body\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.authorId").value(admin.getId().toString()));

        User unenrolled = saveUser(Role.STUDENT, "Unenrolled");
        mockMvc.perform(get("/api/courses/{courseId}/discussion", course.getId())
                        .header("Authorization", bearerToken(unenrolled)))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/courses/{courseId}/discussion", course.getId())
                        .header("Authorization", bearerToken(unenrolled))
                        .contentType("application/json")
                        .content("{\"title\":\"Blocked\",\"body\":\"Blocked body\"}"))
                .andExpect(status().isForbidden());
    }

    private DiscussionPost savePost(Course course, User author, String title, String body) {
        DiscussionPost post = new DiscussionPost();
        post.setCourse(course);
        post.setAuthor(author);
        post.setTitle(title);
        post.setBody(body);
        return discussionPostRepository.saveAndFlush(post);
    }

    private void enroll(User student, Course course) {
        Enrollment enrollment = new Enrollment();
        enrollment.setStudent(student);
        enrollment.setCourse(course);
        enrollmentRepository.saveAndFlush(enrollment);
    }

    private Course saveCourse(User instructor) {
        Course course = new Course();
        course.setTitle("Discussion Course");
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
