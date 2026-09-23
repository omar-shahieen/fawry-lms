package com.fawry.lms.user;

import com.fawry.lms.user.dto.UserProfileResponse;
import com.fawry.lms.user.dto.UpdateCurrentUserRequest;
import com.fawry.lms.user.entities.User;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
@PreAuthorize("hasRole('ADMIN')")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public UserProfileResponse getCurrentUser(@AuthenticationPrincipal User user) {
        return userService.getProfile(user);
    }

    @PatchMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public UserProfileResponse updateCurrentUser(
            @AuthenticationPrincipal User user,
            @RequestBody UpdateCurrentUserRequest request) {
        return userService.updateProfile(user, request);
    }
}
