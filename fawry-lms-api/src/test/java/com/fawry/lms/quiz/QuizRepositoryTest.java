package com.fawry.lms.quiz;

import com.fawry.lms.course.entities.Course;
import com.fawry.lms.course.CourseRepository;
import com.fawry.lms.user.UserRepository;
import com.fawry.lms.quiz.entities.Question;
import com.fawry.lms.quiz.entities.QuestionOption;
import com.fawry.lms.quiz.entities.Quiz;
import com.fawry.lms.quiz.repositories.QuizRepository;
import com.fawry.lms.user.entities.Role;
import com.fawry.lms.user.entities.User;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class QuizRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private QuizRepository quizRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void persistsAndReloadsQuizQuestionOptionTreeByCascade() {
        User instructor = new User();
        instructor.setFullName("Test Instructor");
        instructor.setEmail("instructor@example.com");
        instructor.setPassword("hashed-password");
        instructor.setRole(Role.INSTRUCTOR);
        instructor = userRepository.saveAndFlush(instructor);

        Course course = new Course();
        course.setTitle("Test Course");
        course.setCode("CS301");
        course.setTerm("Fall 2026");
        course.setInstructor(instructor);
        course = courseRepository.saveAndFlush(course);

        Quiz quiz = new Quiz();
        quiz.setCourse(course);
        quiz.setTitle("Module 1 Quiz");
        quiz.setDurationMinutes(30);
        quiz.addQuestion(createQuestion("Question one", 1, "Answer 1", "Distractor 1"));
        quiz.addQuestion(createQuestion("Question two", 2, "Answer 2", "Distractor 2"));
        Long quizId = quizRepository.saveAndFlush(quiz).getId();

        entityManager.clear();
        Quiz reloaded = quizRepository.findById(quizId).orElseThrow();

        assertThat(reloaded.getQuestions()).hasSize(2);
        assertThat(reloaded.getQuestions())
                .extracting(Question::getText)
                .containsExactly("Question one", "Question two");
        assertThat(reloaded.getQuestions())
                .allSatisfy(question -> assertThat(question.getOptions()).hasSize(2));
        assertThat(reloaded.getQuestions().get(0).getOptions())
                .extracting(QuestionOption::isCorrect)
                .containsExactly(true, false);
    }

    private Question createQuestion(String text, int orderIndex, String correctText, String distractorText) {
        Question question = new Question();
        question.setText(text);
        question.setOrderIndex(orderIndex);
        question.addOption(createOption(correctText, true));
        question.addOption(createOption(distractorText, false));
        return question;
    }

    private QuestionOption createOption(String text, boolean correct) {
        QuestionOption option = new QuestionOption();
        option.setText(text);
        option.setCorrect(correct);
        return option;
    }
}