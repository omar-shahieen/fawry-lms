package com.fawry.lms.communication;

import com.fawry.lms.communication.dtos.CreateDiscussionPostRequest;
import com.fawry.lms.communication.dtos.DiscussionPostResponse;
import com.fawry.lms.user.entities.User;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/courses/{courseId}/discussion")
@PreAuthorize("hasRole('ADMIN')")
public class DiscussionController {

    private final DiscussionService discussionService;

    public DiscussionController(DiscussionService discussionService) {
        this.discussionService = discussionService;
    }

    @GetMapping
    @PreAuthorize("@authz.isEnrolledOrStaff(authentication.principal, @courseService.getEntity(#courseId))")
    public Page<DiscussionPostResponse> list(@PathVariable Long courseId, Pageable pageable) {
        return discussionService.list(courseId, pageable);
    }

    @PostMapping
    @PreAuthorize("@authz.isEnrolledOrStaff(authentication.principal, @courseService.getEntity(#courseId))")
    public DiscussionPostResponse create(
            @PathVariable Long courseId,
            @AuthenticationPrincipal User author,
            @Valid @RequestBody CreateDiscussionPostRequest request) {
        return discussionService.create(courseId, author, request);
    }
}
