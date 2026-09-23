package com.fawry.lms.quiz.dto;

public record UpdateQuizRequest(String title, Integer durationMinutes, Boolean published) {
}
