package com.fawry.lms.quiz;

import org.springframework.data.jpa.repository.JpaRepository;

import com.fawry.lms.quiz.entities.QuizAnswer;
import com.fawry.lms.quiz.entities.QuizAttempt;
import java.util.List;

public interface QuizAnswerRepository extends JpaRepository<QuizAnswer, Long> {
    List<QuizAnswer> findByAttempt(QuizAttempt attempt);
}
