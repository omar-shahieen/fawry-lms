package com.fawry.lms.quiz.dto;

import java.util.List;

public record QuizStudentQuestionResponse(Long id, String text, Integer orderIndex, List<QuizStudentOptionResponse> options) {
}
