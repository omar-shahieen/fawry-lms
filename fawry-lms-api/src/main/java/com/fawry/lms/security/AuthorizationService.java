package com.fawry.lms.security;

import com.fawry.lms.course.Course;
import com.fawry.lms.course.EnrollmentRepository;
import com.fawry.lms.user.Role;
import com.fawry.lms.user.User;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component("authz")
public class AuthorizationService {

    private final EnrollmentRepository enrollmentRepository;

    public AuthorizationService(EnrollmentRepository enrollmentRepository) {
        this.enrollmentRepository = enrollmentRepository;
    }

    public boolean isOwnerOrAdmin(User user, UUID ownerId) {
        return user.getRole() == Role.ADMIN || user.getId().equals(ownerId);
    }

    public boolean isEnrolledOrStaff(User user, Course course) {
        if (user.getRole() == Role.ADMIN) {
            return true;
        }
        if (user.getId().equals(course.getInstructor().getId())) {
            return true;
        }
        return enrollmentRepository.existsByCourseAndStudent(course, user);
    }
}
