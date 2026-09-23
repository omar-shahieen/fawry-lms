package com.fawry.lms.quiz.repositories;

import org.springframework.data.jpa.repository.JpaRepository;

import com.fawry.lms.quiz.entities.Question;

public interface QuestionRepository extends JpaRepository<Question, Long> {
}