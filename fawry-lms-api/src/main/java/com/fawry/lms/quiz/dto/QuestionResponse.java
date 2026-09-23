package com.fawry.lms.quiz.dto;

import java.util.List;

public record QuestionResponse(Long id, String text, Integer orderIndex, List<QuestionOptionResponse> options) {
}
