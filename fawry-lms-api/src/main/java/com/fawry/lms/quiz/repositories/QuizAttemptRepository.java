package com.fawry.lms.quiz.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.fawry.lms.quiz.entities.QuizAttempt;

import java.util.Optional;
import java.util.List;
import java.util.UUID;

public interface QuizAttemptRepository extends JpaRepository<QuizAttempt, Long> {

    Optional<QuizAttempt> findByQuizIdAndStudentId(Long quizId, UUID studentId);

    Page<QuizAttempt> findByQuizId(Long quizId, Pageable pageable);

    List<QuizAttempt> findByStudent_Id(UUID studentId);

    Page<QuizAttempt> findByQuiz_Course_Id(Long courseId, Pageable pageable);

    Page<QuizAttempt> findByQuiz_Course_IdAndScoreIsNotNull(Long courseId, Pageable pageable);
}
