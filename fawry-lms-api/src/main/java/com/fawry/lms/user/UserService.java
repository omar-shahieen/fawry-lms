
package com.fawry.lms.user;

import com.fawry.lms.course.EnrollmentRepository;
import com.fawry.lms.user.dtos.EnrolledCourseResponse;
import com.fawry.lms.user.dtos.UserProfileResponse;
import com.fawry.lms.user.dtos.UpdateCurrentUserRequest;
import com.fawry.lms.user.dtos.AdminUserResponse;
import com.fawry.lms.user.dtos.CreateUserRequest;
import com.fawry.lms.user.dtos.AdminUpdateUserRequest;
import com.fawry.lms.user.entities.Role;
import com.fawry.lms.user.entities.User;
import com.fawry.lms.user.repositories.UserRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import com.fawry.lms.user.utils.ProfilePictureUrlGenerator;

@Service
public class UserService {

        private final UserRepository userRepository;
        private final EnrollmentRepository enrollmentRepository;
        private final PasswordEncoder passwordEncoder;
        private final ProfilePictureUrlGenerator profilePictureUrlGenerator;

        public UserService(
                        UserRepository userRepository,
                        EnrollmentRepository enrollmentRepository,
                        PasswordEncoder passwordEncoder,
                        ProfilePictureUrlGenerator profilePictureUrlGenerator) {
                this.userRepository = userRepository;
                this.enrollmentRepository = enrollmentRepository;
                this.passwordEncoder = passwordEncoder;
                this.profilePictureUrlGenerator = profilePictureUrlGenerator;
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

        @Transactional
        public AdminUserResponse createUser(CreateUserRequest request) {
                User user = new User();
                user.setFullName(request.fullName());
                user.setEmail(request.email());
                user.setPassword(passwordEncoder.encode(request.password()));
                user.setRole(request.role());
                user.setActive(true);
                user.setProfilePictureUrl(request.profilePictureUrl() == null
                                ? profilePictureUrlGenerator.generate()
                                : request.profilePictureUrl());
                return toAdminResponse(userRepository.saveAndFlush(user));
        }

        @Transactional
        public AdminUserResponse updateUser(UUID id, AdminUpdateUserRequest request) {
                User user = findUser(id);
                if (request.fullName() != null) {
                        user.setFullName(request.fullName());
                }
                if (request.email() != null) {
                        user.setEmail(request.email());
                }
                if (request.password() != null) {
                        user.setPassword(passwordEncoder.encode(request.password()));
                }
                if (request.role() != null && request.role() != user.getRole()) {
                        user.setRole(request.role());
                        user.setAccessToken(null);
                }
                if (request.profilePictureUrl() != null) {
                        user.setProfilePictureUrl(request.profilePictureUrl());
                }
                if (request.isActive() != null) {
                        user.setActive(request.isActive());
                        if (!request.isActive()) {
                                user.setAccessToken(null);
                        }
                }
                return toAdminResponse(userRepository.saveAndFlush(user));
        }

        @Transactional
        public AdminUserResponse deactivateUser(UUID id) {
                User user = findUser(id);
                user.setActive(false);
                user.setAccessToken(null);
                return toAdminResponse(userRepository.saveAndFlush(user));
        }

        private User findUser(UUID id) {
                return userRepository.findById(id)
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
