package com.fawry.lms.section;

import com.fawry.lms.course.Course;
import com.fawry.lms.course.CourseRepository;
import com.fawry.lms.user.Role;
import com.fawry.lms.user.User;
import com.fawry.lms.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class SectionRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private SectionRepository sectionRepository;

    @Test
    void findsCourseSectionsOrderedByOrderIndex() {
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

        sectionRepository.saveAndFlush(createSection(course, "Second", 2));
        sectionRepository.saveAndFlush(createSection(course, "First", 1));

        assertThat(sectionRepository.findByCourseOrderByOrderIndexAsc(course))
                .extracting(Section::getOrderIndex)
                .containsExactly(1, 2);
    }

    private Section createSection(Course course, String title, int orderIndex) {
        Section section = new Section();
        section.setCourse(course);
        section.setTitle(title);
        section.setOrderIndex(orderIndex);
        return section;
    }
}