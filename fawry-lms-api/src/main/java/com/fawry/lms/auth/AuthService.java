package com.fawry.lms.auth;

import com.fawry.lms.security.JwtTokenProvider;
import com.fawry.lms.user.ProfilePictureUrlGenerator;
import com.fawry.lms.user.Role;
import com.fawry.lms.user.User;
import com.fawry.lms.user.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;
    private final ProfilePictureUrlGenerator profilePictureUrlGenerator;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtTokenProvider tokenProvider,
            ProfilePictureUrlGenerator profilePictureUrlGenerator) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenProvider = tokenProvider;
        this.profilePictureUrlGenerator = profilePictureUrlGenerator;
    }

    @Transactional
    public AuthResponse signup(SignupRequest request) {
        User user = new User();
        user.setFullName(request.fullName());
        user.setEmail(request.email());
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setRole(Role.STUDENT);
        user.setActive(true);
        user.setProfilePictureUrl(profilePictureUrlGenerator.generate());
        user = userRepository.saveAndFlush(user);

        AuthenticatedUserResponse userResponse = new AuthenticatedUserResponse(
                user.getId(), user.getFullName(), user.getEmail(), user.getRole(), user.getProfilePictureUrl());
        return new AuthResponse(
                tokenProvider.generateAccessToken(user.getId(), user.getRole()),
                tokenProvider.generateRefreshToken(user.getId(), user.getRole()),
                userResponse);
    }
}
