package com.fawry.lms.quiz;

import org.springframework.data.jpa.repository.JpaRepository;

import com.fawry.lms.quiz.entities.Quiz;

public interface QuizRepository extends JpaRepository<Quiz, Long> {
}