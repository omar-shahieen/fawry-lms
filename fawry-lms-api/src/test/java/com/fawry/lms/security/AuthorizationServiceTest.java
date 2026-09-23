package com.fawry.lms.security;

import com.fawry.lms.course.Course;
import com.fawry.lms.course.EnrollmentRepository;
import com.fawry.lms.user.Role;
import com.fawry.lms.user.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthorizationServiceTest {

    @Mock
    private EnrollmentRepository enrollmentRepository;

    @Test
    void adminIsAlwaysAllowedAndDoesNotNeedAnEnrollmentLookup() {
        AuthorizationService authorizationService = new AuthorizationService(enrollmentRepository);
        User admin = user(Role.ADMIN, UUID.randomUUID());
        Course course = courseWithInstructor(user(Role.INSTRUCTOR, UUID.randomUUID()));

        assertThat(authorizationService.isOwnerOrAdmin(admin, UUID.randomUUID())).isTrue();
        assertThat(authorizationService.isEnrolledOrStaff(admin, course)).isTrue();
        verifyNoInteractions(enrollmentRepository);
    }

    @Test
    void ownerIsAllowed() {
        AuthorizationService authorizationService = new AuthorizationService(enrollmentRepository);
        UUID ownerId = UUID.randomUUID();

        assertThat(authorizationService.isOwnerOrAdmin(user(Role.INSTRUCTOR, ownerId), ownerId)).isTrue();
        verifyNoInteractions(enrollmentRepository);
    }

    @Test
    void nonOwnerWhoIsNotAdminIsDenied() {
        AuthorizationService authorizationService = new AuthorizationService(enrollmentRepository);

        assertThat(authorizationService.isOwnerOrAdmin(
                user(Role.INSTRUCTOR, UUID.randomUUID()), UUID.randomUUID())).isFalse();
        verifyNoInteractions(enrollmentRepository);
    }

    @Test
    void enrolledStudentIsAllowed() {
        AuthorizationService authorizationService = new AuthorizationService(enrollmentRepository);
        User student = user(Role.STUDENT, UUID.randomUUID());
        Course course = courseWithInstructor(user(Role.INSTRUCTOR, UUID.randomUUID()));
        when(enrollmentRepository.existsByCourseAndStudent(course, student)).thenReturn(true);

        assertThat(authorizationService.isEnrolledOrStaff(student, course)).isTrue();
        verify(enrollmentRepository).existsByCourseAndStudent(course, student);
    }

    @Test
    void courseInstructorIsAllowedWithoutEnrollmentLookup() {
        AuthorizationService authorizationService = new AuthorizationService(enrollmentRepository);
        User instructor = user(Role.INSTRUCTOR, UUID.randomUUID());
        Course course = courseWithInstructor(instructor);

        assertThat(authorizationService.isEnrolledOrStaff(instructor, course)).isTrue();
        verify(enrollmentRepository, never()).existsByCourseAndStudent(course, instructor);
    }

    @Test
    void unrelatedStudentIsDenied() {
        AuthorizationService authorizationService = new AuthorizationService(enrollmentRepository);
        User student = user(Role.STUDENT, UUID.randomUUID());
        Course course = courseWithInstructor(user(Role.INSTRUCTOR, UUID.randomUUID()));
        when(enrollmentRepository.existsByCourseAndStudent(course, student)).thenReturn(false);

        assertThat(authorizationService.isEnrolledOrStaff(student, course)).isFalse();
        verify(enrollmentRepository).existsByCourseAndStudent(course, student);
    }

    private User user(Role role, UUID id) {
        User user = new User();
        user.setRole(role);
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    private Course courseWithInstructor(User instructor) {
        Course course = new Course();
        course.setInstructor(instructor);
        return course;
    }
}
