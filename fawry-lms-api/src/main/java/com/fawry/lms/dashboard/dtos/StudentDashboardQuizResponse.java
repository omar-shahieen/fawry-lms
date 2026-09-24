package com.fawry.lms.dashboard.dtos;

public record StudentDashboardQuizResponse(Long quizId, String quizTitle, boolean attempted, Integer score) {
}
