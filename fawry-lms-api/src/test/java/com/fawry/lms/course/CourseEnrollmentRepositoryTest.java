package com.fawry.lms.course;

import com.fawry.lms.user.Role;
import com.fawry.lms.user.User;
import com.fawry.lms.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class CourseEnrollmentRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private EnrollmentRepository enrollmentRepository;

    @Test
    void courseCodeIsUniqueAcrossTerms() {
        User instructor = userRepository.saveAndFlush(createUser("instructor@example.com", Role.INSTRUCTOR));
        courseRepository.saveAndFlush(createCourse(instructor, "CS301", "Fall 2026"));

        assertThatThrownBy(() -> courseRepository.saveAndFlush(createCourse(instructor, "CS301", "Spring 2027")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void studentCannotBeEnrolledInTheSameCourseTwice() {
        User instructor = userRepository.saveAndFlush(createUser("instructor@example.com", Role.INSTRUCTOR));
        User student = userRepository.saveAndFlush(createUser("student@example.com", Role.STUDENT));
        Course course = courseRepository.saveAndFlush(createCourse(instructor, "CS301", "Fall 2026"));
        enrollmentRepository.saveAndFlush(createEnrollment(student, course));

        assertThatThrownBy(() -> enrollmentRepository.saveAndFlush(createEnrollment(student, course)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    private User createUser(String email, Role role) {
        User user = new User();
        user.setFullName("Test User");
        user.setEmail(email);
        user.setPassword("hashed-password");
        user.setRole(role);
        return user;
    }

    private Course createCourse(User instructor, String code, String term) {
        Course course = new Course();
        course.setTitle("Test Course");
        course.setDescription("Test description");
        course.setCode(code);
        course.setTerm(term);
        course.setInstructor(instructor);
        return course;
    }

    private Enrollment createEnrollment(User student, Course course) {
        Enrollment enrollment = new Enrollment();
        enrollment.setStudent(student);
        enrollment.setCourse(course);
        return enrollment;
    }
}