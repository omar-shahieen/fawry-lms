package com.fawry.lms.user;

import com.fawry.lms.user.dtos.UserProfileResponse;
import com.fawry.lms.user.dtos.UpdateCurrentUserRequest;
import com.fawry.lms.user.dtos.AdminUserResponse;
import com.fawry.lms.user.dtos.CreateUserRequest;
import com.fawry.lms.user.dtos.AdminUpdateUserRequest;
import com.fawry.lms.user.entities.Role;
import com.fawry.lms.user.entities.User;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.UUID;

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
            @Valid @RequestBody UpdateCurrentUserRequest request) {
        return userService.updateProfile(user, request);
    }

    @GetMapping
    public Page<AdminUserResponse> listUsers(
            @RequestParam(required = false) Role role,
            Pageable pageable) {
        return userService.listUsers(role, pageable);
    }

    @GetMapping("/{id}")
    public AdminUserResponse getUser(@PathVariable UUID id) {
        return userService.getUser(id);
    }

    @PostMapping
    public ResponseEntity<AdminUserResponse> createUser(@Valid @RequestBody CreateUserRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(userService.createUser(request));
    }

    @PatchMapping("/{id}")
    public AdminUserResponse updateUser(
            @PathVariable UUID id,
            @Valid @RequestBody AdminUpdateUserRequest request) {
        return userService.updateUser(id, request);
    }

    @PatchMapping("/{id}/deactivate")
    public AdminUserResponse deactivateUser(@PathVariable UUID id) {
        return userService.deactivateUser(id);
    }
}
