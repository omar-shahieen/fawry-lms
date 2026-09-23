package com.fawry.lms.auth;

import com.fawry.lms.security.JwtTokenProvider;
import com.fawry.lms.user.ProfilePictureUrlGenerator;
import com.fawry.lms.user.Role;
import com.fawry.lms.user.User;
import com.fawry.lms.user.UserRepository;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import io.jsonwebtoken.JwtException;

import java.util.UUID;

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

        String accessToken = tokenProvider.generateAccessToken(user.getId(), user.getRole());
        user.setAccessToken(accessToken);
        userRepository.saveAndFlush(user);

        AuthenticatedUserResponse userResponse = new AuthenticatedUserResponse(
                user.getId(), user.getFullName(), user.getEmail(), user.getRole(), user.getProfilePictureUrl());
        return new AuthResponse(
                accessToken,
                tokenProvider.generateRefreshToken(user.getId(), user.getRole()),
                userResponse);
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.email())
                .filter(User::isActive)
                .filter(candidate -> passwordEncoder.matches(request.password(), candidate.getPassword()))
                .orElseThrow(() -> new BadCredentialsException("Invalid email or password."));

        String accessToken = tokenProvider.generateAccessToken(user.getId(), user.getRole());
        user.setAccessToken(accessToken);
        userRepository.saveAndFlush(user);

        AuthenticatedUserResponse userResponse = new AuthenticatedUserResponse(
                user.getId(), user.getFullName(), user.getEmail(), user.getRole(), user.getProfilePictureUrl());
        return new AuthResponse(
                accessToken,
                tokenProvider.generateRefreshToken(user.getId(), user.getRole()),
                userResponse);
    }

    @Transactional
    public RefreshResponse refresh(RefreshRequest request) {
        JwtTokenProvider.TokenClaims claims;
        try {
            claims = tokenProvider.parseToken(request.refreshToken());
        } catch (JwtException | IllegalArgumentException exception) {
            throw new BadCredentialsException("Invalid refresh token.");
        }

        if (claims.tokenType() != JwtTokenProvider.TokenType.REFRESH) {
            throw new BadCredentialsException("Invalid refresh token.");
        }

        User user = userRepository.findById(claims.userId())
                .filter(User::isActive)
                .filter(candidate -> candidate.getRole() == claims.role())
                .filter(candidate -> candidate.getAccessToken() != null)
                .orElseThrow(() -> new BadCredentialsException("Invalid refresh token."));

        String accessToken = tokenProvider.generateAccessToken(user.getId(), user.getRole());
        user.setAccessToken(accessToken);
        userRepository.saveAndFlush(user);
        return new RefreshResponse(accessToken);
    }

    @Transactional
    public void logout(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BadCredentialsException("Invalid authenticated user."));
        user.setAccessToken(null);
        userRepository.saveAndFlush(user);
    }
}
