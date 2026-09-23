package com.fawry.lms.section;

import com.fawry.lms.section.dto.CreateSectionRequest;
import com.fawry.lms.section.dto.SectionResponse;
import com.fawry.lms.section.dto.UpdateSectionRequest;

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
public class SectionController {

    private final SectionService sectionService;

    public SectionController(SectionService sectionService) {
        this.sectionService = sectionService;
    }

    @GetMapping("/api/courses/{courseId}/sections")
    @PreAuthorize("hasRole('ADMIN') or @authz.isEnrolledOrStaff(authentication.principal, @courseService.getEntity(#courseId))")
    public List<SectionResponse> list(@PathVariable Long courseId) {
        return sectionService.list(courseId);
    }

    @PostMapping("/api/courses/{courseId}/sections")
    @PreAuthorize("hasRole('ADMIN') or @authz.isOwnerOrAdmin(authentication.principal, @courseService.getEntity(#courseId))")
    public SectionResponse create(@PathVariable Long courseId, @Valid @RequestBody CreateSectionRequest request) {
        return sectionService.create(courseId, request);
    }

    @PatchMapping("/api/sections/{id}")
    @PreAuthorize("hasRole('ADMIN') or @authz.isOwnerOrAdmin(authentication.principal, @sectionService.getCourse(#id))")
    public SectionResponse update(@PathVariable Long id, @RequestBody UpdateSectionRequest request) {
        return sectionService.update(id, request);
    }

    @DeleteMapping("/api/sections/{id}")
    @PreAuthorize("hasRole('ADMIN') or @authz.isOwnerOrAdmin(authentication.principal, @sectionService.getCourse(#id))")
    public void delete(@PathVariable Long id) {
        sectionService.delete(id);
    }
}
