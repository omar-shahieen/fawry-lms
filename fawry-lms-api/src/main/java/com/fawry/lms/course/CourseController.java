package com.fawry.lms.course;

import com.fawry.lms.course.dtos.CourseResponse;
import com.fawry.lms.course.dtos.CreateCourseRequest;
import com.fawry.lms.course.dtos.UpdateCourseRequest;
import com.fawry.lms.course.dtos.AssignInstructorRequest;
import com.fawry.lms.course.dtos.CourseStudentResponse;
import com.fawry.lms.course.dtos.EnrollmentResponse;
import com.fawry.lms.user.entities.User;

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
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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

    @PatchMapping("/{id}/assign-instructor")
    public CourseResponse assignInstructor(
            @PathVariable Long id,
            @Valid @RequestBody AssignInstructorRequest request) {
        return courseService.assignInstructor(id, request);
    }

    @PostMapping("/{id}/enroll")
    @PreAuthorize("hasRole('STUDENT')")
    public EnrollmentResponse enroll(
            @PathVariable Long id,
            @AuthenticationPrincipal User student) {
        return courseService.enroll(id, student);
    }

    @GetMapping("/{id}/students")
    @PreAuthorize("@authz.isOwnerOrAdmin(authentication.principal, @courseService.getEntity(#id))")
    public Page<CourseStudentResponse> listStudents(
            @PathVariable Long id,
            Pageable pageable) {
        return courseService.listStudents(id, pageable);
    }
}
