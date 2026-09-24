package com.fawry.lms.communication;

import com.fawry.lms.communication.dtos.CreateDiscussionPostRequest;
import com.fawry.lms.communication.dtos.CreateDiscussionReplyRequest;
import com.fawry.lms.communication.dtos.DiscussionPostResponse;
import com.fawry.lms.communication.dtos.DiscussionReplyResponse;
import com.fawry.lms.communication.dtos.UpdateDiscussionPostRequest;
import com.fawry.lms.user.entities.User;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping
@PreAuthorize("hasRole('ADMIN')")
public class DiscussionController {

    private final DiscussionService discussionService;

    public DiscussionController(DiscussionService discussionService) {
        this.discussionService = discussionService;
    }

    @GetMapping("/api/courses/{courseId}/discussion")
    @PreAuthorize("@authz.isEnrolledOrStaff(authentication.principal, @courseService.getEntity(#courseId))")
    public Page<DiscussionPostResponse> list(@PathVariable Long courseId, Pageable pageable) {
        return discussionService.list(courseId, pageable);
    }

    @PostMapping("/api/courses/{courseId}/discussion")
    @PreAuthorize("@authz.isEnrolledOrStaff(authentication.principal, @courseService.getEntity(#courseId))")
    public DiscussionPostResponse create(
            @PathVariable Long courseId,
            @AuthenticationPrincipal User author,
            @Valid @RequestBody CreateDiscussionPostRequest request) {
        return discussionService.create(courseId, author, request);
    }

    @PostMapping("/api/discussion/{postId}/reply")
    @PreAuthorize("@authz.isEnrolledOrStaff(authentication.principal, @discussionService.getCourse(#postId))")
    public DiscussionReplyResponse reply(
            @PathVariable Long postId,
            @AuthenticationPrincipal User author,
            @Valid @RequestBody CreateDiscussionReplyRequest request) {
        return discussionService.reply(postId, author, request);
    }

    @PatchMapping("/api/discussion/{id}")
    @PreAuthorize("@authz.isOwner(authentication.principal, @discussionService.getAuthorId(#id))")
    public void update(@PathVariable Long id, @Valid @RequestBody UpdateDiscussionPostRequest request) {
        discussionService.update(id, request);
    }

    @DeleteMapping("/api/discussion/{id}")
    @PreAuthorize("hasRole('ADMIN') or @authz.isOwner(authentication.principal, @discussionService.getAuthorId(#id))")
    public void delete(@PathVariable Long id) {
        discussionService.delete(id);
    }
}
