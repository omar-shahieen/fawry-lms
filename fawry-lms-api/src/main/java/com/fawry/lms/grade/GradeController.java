package com.fawry.lms.grade;

import com.fawry.lms.grade.dtos.CourseGradesResponse;
import com.fawry.lms.grade.dtos.CourseGradeResponse;
import com.fawry.lms.user.entities.User;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@RestController
@RequestMapping
@PreAuthorize("hasRole('ADMIN')")
public class GradeController {

    private final GradeService gradeService;

    public GradeController(GradeService gradeService) {
        this.gradeService = gradeService;
    }

    @GetMapping("/api/students/me/grades")
    @PreAuthorize("hasRole('STUDENT')")
    public List<CourseGradesResponse> myGrades(@AuthenticationPrincipal User student) {
        return gradeService.getMyGrades(student);
    }

    @GetMapping("/api/courses/{courseId}/grades")
    @PreAuthorize("hasRole('ADMIN') or @authz.isOwnerOrAdmin(authentication.principal, @courseService.getEntity(#courseId))")
    public Page<CourseGradeResponse> courseGrades(@PathVariable Long courseId, Pageable pageable) {
        return gradeService.getCourseGrades(courseId, pageable);
    }
}
