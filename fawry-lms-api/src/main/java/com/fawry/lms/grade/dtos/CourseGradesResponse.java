package com.fawry.lms.grade.dtos;

import java.util.List;

public record CourseGradesResponse(
        Long courseId,
        String courseTitle,
        String courseCode,
        List<QuizGradeResponse> quizzes) {
}
