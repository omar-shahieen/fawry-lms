package com.fawry.lms.quiz.dtos;

import java.util.List;

public record QuestionResponse(Long id, String text, Integer orderIndex, List<QuestionOptionResponse> options) {
}
