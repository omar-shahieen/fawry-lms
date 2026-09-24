package com.fawry.lms.dashboard.dtos;

import java.util.List;

public record StudentDashboardCourseResponse(
        Long courseId,
        String courseName,
        List<StudentDashboardQuizResponse> quizzes) {
}
