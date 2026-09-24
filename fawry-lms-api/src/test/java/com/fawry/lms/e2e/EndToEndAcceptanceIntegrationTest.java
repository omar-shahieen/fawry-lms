package com.fawry.lms.e2e;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "lms.seed.enabled=true",
        "ADMIN_SEED_EMAIL=admin@lms.com",
        "ADMIN_SEED_PASSWORD=Admin123!"
})
@ActiveProfiles("test")
@AutoConfigureMockMvc
class EndToEndAcceptanceIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void completesTheTwentySevenStepApiAcceptanceWorkflow() throws Exception {
        // 1. Seeded administrator can log in.
        MvcResult adminLogin = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"admin@lms.com","password":"Admin123!"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.role").value("ADMIN"))
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andReturn();
        String adminToken = text(adminLogin, "$.accessToken");

        // 2. A signup carrying a forged role still creates a student.
        MvcResult signup = mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"fullName":"E2E Student","email":"e2e-student@example.com","password":"Student123!","role":"ADMIN"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.user.role").value("STUDENT"))
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andReturn();
        String studentToken = text(signup, "$.accessToken");

        // 3. Admin creates an instructor.
        MvcResult instructorCreation = mockMvc.perform(post("/api/users")
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"fullName":"E2E Instructor","email":"e2e-instructor@example.com","password":"Instructor123!","role":"INSTRUCTOR"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("INSTRUCTOR"))
                .andReturn();
        String instructorId = text(instructorCreation, "$.id");
        String instructorToken = login("e2e-instructor@example.com", "Instructor123!");

        // 4. Admin creates a course assigned to that instructor.
        MvcResult courseCreation = mockMvc.perform(post("/api/courses")
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"E2E Course","description":"Acceptance workflow course","code":"E2E-101","term":"Fall 2026","instructorId":"%s"}
                                """.formatted(instructorId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("E2E Course"))
                .andReturn();
        String courseId = text(courseCreation, "$.id");

        // 5. Admin assigns the instructor through the assignment endpoint.
        mockMvc.perform(patch("/api/courses/{id}/assign-instructor", courseId)
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"instructorId\":\"%s\"}".formatted(instructorId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.instructorId").value(instructorId));

        // 6. The student enrolls in the course.
        mockMvc.perform(post("/api/courses/{id}/enroll", courseId)
                        .header("Authorization", bearer(studentToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.courseId").value(Long.valueOf(courseId)));

        // 7. Instructor creates a section.
        MvcResult sectionCreation = mockMvc.perform(post("/api/courses/{id}/sections", courseId)
                        .header("Authorization", bearer(instructorToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"E2E Section\",\"orderIndex\":1}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("E2E Section"))
                .andReturn();
        String sectionId = text(sectionCreation, "$.id");

        // 8. Instructor adds Markdown content.
        MvcResult contentCreation = mockMvc.perform(post("/api/sections/{id}/content", sectionId)
                        .header("Authorization", bearer(instructorToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"E2E Notes\",\"body\":\"# Notes\\nSample markdown.\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("E2E Notes"))
                .andReturn();
        String contentId = text(contentCreation, "$.id");

        // 9. Instructor creates an unpublished timed quiz.
        MvcResult quizCreation = mockMvc.perform(post("/api/courses/{id}/quizzes", courseId)
                        .header("Authorization", bearer(instructorToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"E2E Quiz\",\"durationMinutes\":20,\"published\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.published").value(false))
                .andReturn();
        String quizId = text(quizCreation, "$.id");

        // 10. Instructor adds two multiple-choice questions.
        MvcResult firstQuestion = addQuestion(instructorToken, quizId, 1, "First question?");
        MvcResult secondQuestion = addQuestion(instructorToken, quizId, 2, "Second question?");
        String firstQuestionId = text(firstQuestion, "$.id");
        String secondQuestionId = text(secondQuestion, "$.id");
        String firstCorrectOptionId = text(firstQuestion, "$.options[1].id");
        String secondCorrectOptionId = text(secondQuestion, "$.options[1].id");

        // 11. Instructor publishes the quiz.
        mockMvc.perform(patch("/api/quizzes/{id}", quizId)
                        .header("Authorization", bearer(instructorToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"published\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.published").value(true));

        // 12. Instructor posts an announcement.
        MvcResult announcementCreation = mockMvc.perform(post("/api/courses/{id}/announcements", courseId)
                        .header("Authorization", bearer(instructorToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"E2E Announcement\",\"body\":\"The course is ready.\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("E2E Announcement"))
                .andReturn();
        String announcementId = text(announcementCreation, "$.id");

        // 13. Student retrieves the enrolled course.
        mockMvc.perform(get("/api/courses/{id}", courseId)
                        .header("Authorization", bearer(studentToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("E2E Course"));

        // 14. Student retrieves the section and Markdown content.
        mockMvc.perform(get("/api/courses/{id}/sections", courseId)
                        .header("Authorization", bearer(studentToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(Long.valueOf(sectionId)));
        mockMvc.perform(get("/api/content/{id}", contentId)
                        .header("Authorization", bearer(studentToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.body").value("# Notes\nSample markdown."));

        // 15. First student quiz GET starts a timed attempt and hides answer keys.
        MvcResult firstQuizView = mockMvc.perform(get("/api/quizzes/{id}", quizId)
                        .header("Authorization", bearer(studentToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.startedAt").isNotEmpty())
                .andExpect(jsonPath("$.expiresAt").isNotEmpty())
                .andExpect(jsonPath("$.questions.length()").value(2))
                .andExpect(jsonPath("$.questions[0].options[0].isCorrect").doesNotExist())
                .andReturn();
        String firstStartedAt = text(firstQuizView, "$.startedAt");
        String firstExpiresAt = text(firstQuizView, "$.expiresAt");

        // 16. Re-fetching before submission does not reset the attempt timer.
        mockMvc.perform(get("/api/quizzes/{id}", quizId)
                        .header("Authorization", bearer(studentToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.startedAt").value(firstStartedAt))
                .andExpect(jsonPath("$.expiresAt").value(firstExpiresAt));

        String answers = """
                {"answers":[{"questionId":%s,"selectedOptionId":%s},{"questionId":%s,"selectedOptionId":%s}]}
                """.formatted(firstQuestionId, firstCorrectOptionId, secondQuestionId, secondCorrectOptionId);

        // 17. Server grades the submission.
        mockMvc.perform(post("/api/quizzes/{id}/submit", quizId)
                        .header("Authorization", bearer(studentToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(answers))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.score").value(2))
                .andExpect(jsonPath("$.totalQuestions").value(2));

        // 18. The single-attempt rule rejects a second submission.
        mockMvc.perform(post("/api/quizzes/{id}/submit", quizId)
                        .header("Authorization", bearer(studentToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(answers))
                .andExpect(status().isConflict());

        // 19. Student retrieves the saved attempt result/history.
        mockMvc.perform(get("/api/quizzes/{id}/attempts/me", quizId)
                        .header("Authorization", bearer(studentToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.score").value(2))
                .andExpect(jsonPath("$.totalQuestions").value(2));

        // 20. Student sees cross-course grades and the instructor sees course grades.
        mockMvc.perform(get("/api/students/me/grades")
                        .header("Authorization", bearer(studentToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.courseId == " + courseId + ")].quizzes[0].score").value(2));
        mockMvc.perform(get("/api/courses/{id}/grades", courseId)
                        .header("Authorization", bearer(instructorToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].score").value(2));

        // 21. Student creates a discussion post.
        MvcResult postCreation = mockMvc.perform(post("/api/courses/{id}/discussion", courseId)
                        .header("Authorization", bearer(studentToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"E2E question\",\"body\":\"Can you explain the topic?\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.body").value("Can you explain the topic?"))
                .andReturn();
        String postId = text(postCreation, "$.id");

        // 22. Instructor replies to the student's post.
        MvcResult replyCreation = mockMvc.perform(post("/api/discussion/{id}/reply", postId)
                        .header("Authorization", bearer(instructorToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"body\":\"Review the Markdown notes.\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.body").value("Review the Markdown notes."))
                .andReturn();
        String replyId = text(replyCreation, "$.id");

        // 23. A reply-to-a-reply is rejected.
        mockMvc.perform(post("/api/discussion/{id}/reply", replyId)
                        .header("Authorization", bearer(studentToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"body\":\"Nested reply is not allowed.\"}"))
                .andExpect(status().isBadRequest());

        // 24. Student retrieves the course announcement.
        mockMvc.perform(get("/api/courses/{id}/announcements", courseId)
                        .header("Authorization", bearer(studentToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(Long.valueOf(announcementId)))
                .andExpect(jsonPath("$.content[0].title").value("E2E Announcement"));

        // 25. Student dashboard includes the enrolled course and quiz.
        mockMvc.perform(get("/api/students/me/dashboard")
                        .header("Authorization", bearer(studentToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.courseId == " + courseId + ")].courseName").value("E2E Course"));

        // 26. Instructor dashboard includes the assigned course.
        mockMvc.perform(get("/api/instructors/me/dashboard")
                        .header("Authorization", bearer(instructorToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.courses[?(@.courseId == " + courseId + ")].courseName")
                        .value("E2E Course"));

        // 27. Admin dashboard, course listing, and user listing include the new data.
        mockMvc.perform(get("/api/admin/dashboard")
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCourseCount").value(3))
                .andExpect(jsonPath("$.userCountsByRole.STUDENT").value(7))
                .andExpect(jsonPath("$.userCountsByRole.INSTRUCTOR").value(3));
        mockMvc.perform(get("/api/courses")
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(3));
        mockMvc.perform(get("/api/users")
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(11));
    }

    private MvcResult addQuestion(String instructorToken, String quizId, int order, String text)
            throws Exception {
        return mockMvc.perform(post("/api/quizzes/{id}/questions", quizId)
                        .header("Authorization", bearer(instructorToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"text":"%s","orderIndex":%d,"options":[{"text":"Incorrect option","isCorrect":false},{"text":"Correct option","isCorrect":true}]}
                                """.formatted(text, order)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.options.length()").value(2))
                .andReturn();
    }

    private String login(String email, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"%s\",\"password\":\"%s\"}".formatted(email, password)))
                .andExpect(status().isOk())
                .andReturn();
        return text(result, "$.accessToken");
    }

    private String text(MvcResult result, String path) throws Exception {
        return JsonPath.read(result.getResponse().getContentAsString(), path).toString();
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }
}
