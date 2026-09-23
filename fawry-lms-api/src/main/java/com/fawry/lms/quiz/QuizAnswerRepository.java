package com.fawry.lms.quiz;

import org.springframework.data.jpa.repository.JpaRepository;

import com.fawry.lms.quiz.entities.QuizAnswer;

public interface QuizAnswerRepository extends JpaRepository<QuizAnswer, Long> {
}
