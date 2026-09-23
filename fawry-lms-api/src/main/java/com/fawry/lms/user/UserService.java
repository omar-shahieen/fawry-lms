
package com.fawry.lms.user;

import com.fawry.lms.course.EnrollmentRepository;
import com.fawry.lms.user.dto.EnrolledCourseResponse;
import com.fawry.lms.user.dto.UserProfileResponse;
import com.fawry.lms.user.entities.Role;
import com.fawry.lms.user.entities.User;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class UserService {

        private final EnrollmentRepository enrollmentRepository;

        public UserService(EnrollmentRepository enrollmentRepository) {
                this.enrollmentRepository = enrollmentRepository;
        }

        @Transactional(readOnly = true)
        public UserProfileResponse getProfile(User user) {
                List<EnrolledCourseResponse> enrolledCourses = user.getRole() == Role.STUDENT
                                ? enrollmentRepository.findByStudent(user).stream()
                                                .map(enrollment -> new EnrolledCourseResponse(
                                                                enrollment.getCourse().getId(),
                                                                enrollment.getCourse().getTitle()))
                                                .toList()
                                : List.of();

                return new UserProfileResponse(
                                user.getId(),
                                user.getFullName(),
                                user.getEmail(),
                                user.getRole(),
                                user.getProfilePictureUrl(),
                                enrolledCourses);
        }
}
