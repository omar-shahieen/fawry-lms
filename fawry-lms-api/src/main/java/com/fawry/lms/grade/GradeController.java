package com.fawry.lms.grade;

import com.fawry.lms.grade.dtos.CourseGradesResponse;
import com.fawry.lms.user.entities.User;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/students/me/grades")
@PreAuthorize("hasRole('ADMIN')")
public class GradeController {

    private final GradeService gradeService;

    public GradeController(GradeService gradeService) {
        this.gradeService = gradeService;
    }

    @GetMapping
    @PreAuthorize("hasRole('STUDENT')")
    public List<CourseGradesResponse> myGrades(@AuthenticationPrincipal User student) {
        return gradeService.getMyGrades(student);
    }
}
