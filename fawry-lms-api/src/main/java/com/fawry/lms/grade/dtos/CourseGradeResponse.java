package com.fawry.lms.grade.dtos;

import java.time.Instant;

public record CourseGradeResponse(
        GradeStudentResponse student,
        GradeQuizResponse quiz,
        Integer score,
        Integer totalQuestions,
        Instant submittedAt) {
}
