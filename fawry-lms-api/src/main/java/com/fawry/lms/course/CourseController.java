package com.fawry.lms.course;

import com.fawry.lms.course.dto.CourseResponse;
import com.fawry.lms.course.dto.CreateCourseRequest;
import com.fawry.lms.course.dto.UpdateCourseRequest;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestBody;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PathVariable;

@RestController
@RequestMapping("/api/courses")
@PreAuthorize("hasRole('ADMIN')")
public class CourseController {

    private final CourseService courseService;

    public CourseController(CourseService courseService) {
        this.courseService = courseService;
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public Page<CourseResponse> list(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String term,
            @RequestParam(required = false) String code,
            Pageable pageable) {
        return courseService.listActive(search, term, code, pageable);
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public CourseResponse getById(@PathVariable Long id) {
        return courseService.getById(id);
    }

    @PostMapping
    public CourseResponse create(@Valid @RequestBody CreateCourseRequest request) {
        return courseService.create(request);
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or @authz.isOwnerOrAdmin(authentication.principal, @courseService.getInstructorId(#id))")
    public CourseResponse update(@PathVariable Long id, @RequestBody UpdateCourseRequest request) {
        return courseService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public CourseResponse deactivate(@PathVariable Long id) {
        return courseService.deactivate(id);
    }
}
