package com.fawry.lms.quiz.repositories;

import org.springframework.data.jpa.repository.JpaRepository;

import com.fawry.lms.quiz.entities.QuizAttempt;

import java.util.Optional;
import java.util.UUID;

public interface QuizAttemptRepository extends JpaRepository<QuizAttempt, Long> {

    Optional<QuizAttempt> findByQuizIdAndStudentId(Long quizId, UUID studentId);
}
