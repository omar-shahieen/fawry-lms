package com.fawry.lms.section;

import com.fawry.lms.section.dto.ContentResponse;
import com.fawry.lms.section.dto.CreateContentRequest;
import com.fawry.lms.section.dto.UpdateContentRequest;

import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping
@PreAuthorize("hasRole('ADMIN')")
public class ContentController {

    private final ContentService contentService;

    public ContentController(ContentService contentService) {
        this.contentService = contentService;
    }

    @GetMapping("/api/sections/{sectionId}/content")
    @PreAuthorize("hasRole('ADMIN') or @authz.isEnrolledOrStaff(authentication.principal, @sectionService.getCourse(#sectionId))")
    public List<ContentResponse> list(@PathVariable Long sectionId) {
        return contentService.list(sectionId);
    }

    @GetMapping("/api/content/{id}")
    @PreAuthorize("hasRole('ADMIN') or @authz.isEnrolledOrStaff(authentication.principal, @contentService.getCourse(#id))")
    public ContentResponse get(@PathVariable Long id) {
        return contentService.get(id);
    }

    @PostMapping("/api/sections/{sectionId}/content")
    @PreAuthorize("hasRole('ADMIN') or @authz.isOwnerOrAdmin(authentication.principal, @sectionService.getCourse(#sectionId))")
    public ContentResponse create(@PathVariable Long sectionId, @Valid @RequestBody CreateContentRequest request) {
        return contentService.create(sectionId, request);
    }

    @PatchMapping("/api/content/{id}")
    @PreAuthorize("hasRole('ADMIN') or @authz.isOwnerOrAdmin(authentication.principal, @contentService.getCourse(#id))")
    public ContentResponse update(@PathVariable Long id, @RequestBody UpdateContentRequest request) {
        return contentService.update(id, request);
    }

    @DeleteMapping("/api/content/{id}")
    @PreAuthorize("hasRole('ADMIN') or @authz.isOwnerOrAdmin(authentication.principal, @contentService.getCourse(#id))")
    public void delete(@PathVariable Long id) {
        contentService.delete(id);
    }
}
