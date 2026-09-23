package com.fawry.lms.quiz.dtos;

public record UpdateQuizRequest(String title, Integer durationMinutes, Boolean published) {
}
