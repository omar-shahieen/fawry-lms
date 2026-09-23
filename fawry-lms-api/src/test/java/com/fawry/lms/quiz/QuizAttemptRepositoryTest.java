package com.fawry.lms.quiz;

import com.fawry.lms.course.entities.Course;
import com.fawry.lms.course.CourseRepository;
import com.fawry.lms.user.UserRepository;
import com.fawry.lms.quiz.entities.Quiz;
import com.fawry.lms.quiz.entities.QuizAttempt;
import com.fawry.lms.user.entities.Role;
import com.fawry.lms.user.entities.User;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class QuizAttemptRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private QuizRepository quizRepository;

    @Autowired
    private QuizAttemptRepository quizAttemptRepository;

    @Test
    void rejectsSecondAttemptForSameQuizAndStudent() {
        User instructor = createUser("instructor@example.com", Role.INSTRUCTOR);
        User student = createUser("student@example.com", Role.STUDENT);

        Course course = new Course();
        course.setTitle("Test Course");
        course.setCode("CS401");
        course.setTerm("Fall 2026");
        course.setInstructor(instructor);
        course = courseRepository.saveAndFlush(course);

        Quiz quiz = new Quiz();
        quiz.setCourse(course);
        quiz.setTitle("Test Quiz");
        quiz.setDurationMinutes(30);
        quiz = quizRepository.saveAndFlush(quiz);

        QuizAttempt attempt = new QuizAttempt();
        attempt.setQuiz(quiz);
        attempt.setStudent(student);
        quizAttemptRepository.saveAndFlush(attempt);

        QuizAttempt duplicate = new QuizAttempt();
        duplicate.setQuiz(quiz);
        duplicate.setStudent(student);

        assertThatThrownBy(() -> quizAttemptRepository.saveAndFlush(duplicate))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    private User createUser(String email, Role role) {
        User user = new User();
        user.setFullName("Test " + role);
        user.setEmail(email);
        user.setPassword("hashed-password");
        user.setRole(role);
        return userRepository.saveAndFlush(user);
    }
}
