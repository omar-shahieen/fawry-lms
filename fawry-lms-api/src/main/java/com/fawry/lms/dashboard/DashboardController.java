package com.fawry.lms.dashboard;

import com.fawry.lms.dashboard.dtos.StudentDashboardCourseResponse;
import com.fawry.lms.dashboard.dtos.InstructorDashboardResponse;
import com.fawry.lms.user.entities.User;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping
@PreAuthorize("hasRole('ADMIN')")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/api/students/me/dashboard")
    @PreAuthorize("hasRole('STUDENT')")
    public List<StudentDashboardCourseResponse> studentDashboard(@AuthenticationPrincipal User student) {
        return dashboardService.getStudentDashboard(student);
    }

    @GetMapping("/api/instructors/me/dashboard")
    @PreAuthorize("hasRole('INSTRUCTOR')")
    public InstructorDashboardResponse instructorDashboard(@AuthenticationPrincipal User instructor) {
        return dashboardService.getInstructorDashboard(instructor);
    }
}
