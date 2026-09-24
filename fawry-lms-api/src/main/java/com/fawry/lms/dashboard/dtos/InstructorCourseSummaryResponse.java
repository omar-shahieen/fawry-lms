package com.fawry.lms.dashboard.dtos;

public record InstructorCourseSummaryResponse(
        Long courseId,
        String courseName,
        long submittedAttemptCount,
        Double averageScore) {
}
