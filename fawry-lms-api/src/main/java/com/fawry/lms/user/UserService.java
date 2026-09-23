
package com.fawry.lms.user;

import com.fawry.lms.course.EnrollmentRepository;
import com.fawry.lms.user.dto.EnrolledCourseResponse;
import com.fawry.lms.user.dto.UserProfileResponse;
import com.fawry.lms.user.dto.UpdateCurrentUserRequest;
import com.fawry.lms.user.dto.AdminUserResponse;
import com.fawry.lms.user.entities.Role;
import com.fawry.lms.user.entities.User;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import jakarta.persistence.EntityNotFoundException;

@Service
public class UserService {

        private final UserRepository userRepository;
        private final EnrollmentRepository enrollmentRepository;

        public UserService(UserRepository userRepository, EnrollmentRepository enrollmentRepository) {
                this.userRepository = userRepository;
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

        @Transactional
        public UserProfileResponse updateProfile(User user, UpdateCurrentUserRequest request) {
                if (request.fullName() != null) {
                        user.setFullName(request.fullName());
                }
                if (request.profilePictureUrl() != null) {
                        user.setProfilePictureUrl(request.profilePictureUrl());
                }
                return getProfile(user);
        }

        @Transactional(readOnly = true)
        public Page<AdminUserResponse> listUsers(Role role, Pageable pageable) {
                Page<User> users = role == null
                                ? userRepository.findAll(pageable)
                                : userRepository.findByRole(role, pageable);
                return users.map(this::toAdminResponse);
        }

        @Transactional(readOnly = true)
        public AdminUserResponse getUser(UUID id) {
                return userRepository.findById(id)
                                .map(this::toAdminResponse)
                                .orElseThrow(() -> new EntityNotFoundException("User not found."));
        }

        private AdminUserResponse toAdminResponse(User user) {
                return new AdminUserResponse(
                                user.getId(),
                                user.getFullName(),
                                user.getEmail(),
                                user.getRole(),
                                user.getProfilePictureUrl(),
                                user.isActive(),
                                user.getCreatedAt(),
                                user.getUpdatedAt());
        }
}
