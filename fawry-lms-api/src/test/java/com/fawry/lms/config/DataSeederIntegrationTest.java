package com.fawry.lms.config;

import com.fawry.lms.communication.AnnouncementRepository;
import com.fawry.lms.communication.DiscussionPostRepository;
import com.fawry.lms.course.CourseRepository;
import com.fawry.lms.course.EnrollmentRepository;
import com.fawry.lms.quiz.repositories.QuestionOptionRepository;
import com.fawry.lms.quiz.repositories.QuestionRepository;
import com.fawry.lms.quiz.repositories.QuizAnswerRepository;
import com.fawry.lms.quiz.repositories.QuizAttemptRepository;
import com.fawry.lms.quiz.repositories.QuizRepository;
import com.fawry.lms.section.repositories.MarkdownContentRepository;
import com.fawry.lms.section.repositories.SectionRepository;
import com.fawry.lms.user.entities.Role;
import com.fawry.lms.user.repositories.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "lms.seed.enabled=true",
        "ADMIN_SEED_EMAIL=admin@lms.com",
        "ADMIN_SEED_PASSWORD=Admin123!"
})
@ActiveProfiles("test")
class DataSeederIntegrationTest {

    @Autowired private DataSeeder dataSeeder;
    @Autowired private UserRepository userRepository;
    @Autowired private CourseRepository courseRepository;
    @Autowired private EnrollmentRepository enrollmentRepository;
    @Autowired private SectionRepository sectionRepository;
    @Autowired private MarkdownContentRepository contentRepository;
    @Autowired private QuizRepository quizRepository;
    @Autowired private QuestionRepository questionRepository;
    @Autowired private QuestionOptionRepository optionRepository;
    @Autowired private QuizAttemptRepository attemptRepository;
    @Autowired private QuizAnswerRepository answerRepository;
    @Autowired private AnnouncementRepository announcementRepository;
    @Autowired private DiscussionPostRepository discussionPostRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    @Test
    void seedsDemoDataOnceAndSkipsAllInsertsOnSubsequentRuns() {
        assertThat(userRepository.count()).isEqualTo(9);
        assertThat(userRepository.countByRole(Role.ADMIN)).isEqualTo(1);
        assertThat(userRepository.countByRole(Role.INSTRUCTOR)).isEqualTo(2);
        assertThat(userRepository.countByRole(Role.STUDENT)).isEqualTo(6);
        assertThat(courseRepository.count()).isEqualTo(2);
        assertThat(enrollmentRepository.count()).isEqualTo(8);
        assertThat(sectionRepository.count()).isEqualTo(4);
        assertThat(contentRepository.count()).isEqualTo(4);
        assertThat(quizRepository.count()).isEqualTo(2);
        assertThat(questionRepository.count()).isEqualTo(6);
        assertThat(optionRepository.count()).isEqualTo(12);
        assertThat(attemptRepository.count()).isEqualTo(3);
        assertThat(answerRepository.count()).isEqualTo(9);
        assertThat(announcementRepository.count()).isEqualTo(4);
        assertThat(discussionPostRepository.count()).isEqualTo(4);
        assertThat(quizRepository.findAll()).allMatch(quiz -> quiz.isPublished());
        assertThat(userRepository.findAll()).allMatch(user -> user.getProfilePictureUrl().contains("api.dicebear.com"));
        assertThat(courseRepository.findAll()).extracting(course -> course.getInstructor().getId()).doesNotHaveDuplicates();
        assertThat(discussionPostRepository.findAll().stream()
                .filter(post -> post.getParentPost() != null).count()).isEqualTo(2);

        var admin = userRepository.findByEmail("admin@lms.com").orElseThrow();
        assertThat(passwordEncoder.matches("Admin123!", admin.getPassword())).isTrue();
        assertThat(passwordEncoder.matches("Student123!",
                userRepository.findByEmail("student1@lms.com").orElseThrow().getPassword())).isTrue();
        assertThat(enrollmentRepository.findByStudent(userRepository.findByEmail("student1@lms.com").orElseThrow()))
                .hasSizeGreaterThan(1);

        dataSeeder.run();

        assertThat(userRepository.count()).isEqualTo(9);
        assertThat(courseRepository.count()).isEqualTo(2);
        assertThat(enrollmentRepository.count()).isEqualTo(8);
        assertThat(sectionRepository.count()).isEqualTo(4);
        assertThat(contentRepository.count()).isEqualTo(4);
        assertThat(quizRepository.count()).isEqualTo(2);
        assertThat(questionRepository.count()).isEqualTo(6);
        assertThat(optionRepository.count()).isEqualTo(12);
        assertThat(attemptRepository.count()).isEqualTo(3);
        assertThat(answerRepository.count()).isEqualTo(9);
        assertThat(announcementRepository.count()).isEqualTo(4);
        assertThat(discussionPostRepository.count()).isEqualTo(4);
    }
}
