package com.fawry.lms.communication;

import com.fawry.lms.communication.dtos.AnnouncementResponse;
import com.fawry.lms.communication.dtos.CreateAnnouncementRequest;
import com.fawry.lms.communication.dtos.UpdateAnnouncementRequest;
import com.fawry.lms.user.entities.User;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping
@PreAuthorize("hasRole('ADMIN')")
public class AnnouncementController {

    private final AnnouncementService announcementService;

    public AnnouncementController(AnnouncementService announcementService) {
        this.announcementService = announcementService;
    }

    @GetMapping("/api/courses/{courseId}/announcements")
    @PreAuthorize("hasRole('ADMIN') or @authz.isEnrolledOrStaff(authentication.principal, @courseService.getEntity(#courseId))")
    public Page<AnnouncementResponse> list(@PathVariable Long courseId, Pageable pageable) {
        return announcementService.list(courseId, pageable);
    }

    @PostMapping("/api/courses/{courseId}/announcements")
    @PreAuthorize("hasRole('ADMIN') or @authz.isOwnerOrAdmin(authentication.principal, @courseService.getEntity(#courseId))")
    public AnnouncementResponse create(
            @PathVariable Long courseId,
            @AuthenticationPrincipal User author,
            @Valid @RequestBody CreateAnnouncementRequest request) {
        return announcementService.create(courseId, author, request);
    }

    @PatchMapping("/api/announcements/{id}")
    @PreAuthorize("hasRole('ADMIN') or @authz.isOwnerOrAdmin(authentication.principal, @announcementService.getCourse(#id))")
    public AnnouncementResponse update(
            @PathVariable Long id,
            @Valid @RequestBody UpdateAnnouncementRequest request) {
        return announcementService.update(id, request);
    }

    @DeleteMapping("/api/announcements/{id}")
    @PreAuthorize("hasRole('ADMIN') or @authz.isOwnerOrAdmin(authentication.principal, @announcementService.getCourse(#id))")
    public void delete(@PathVariable Long id) {
        announcementService.delete(id);
    }
}
