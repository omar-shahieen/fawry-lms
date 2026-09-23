package com.fawry.lms.quiz.repositories;

import org.springframework.data.jpa.repository.JpaRepository;

import com.fawry.lms.quiz.entities.QuestionOption;

public interface QuestionOptionRepository extends JpaRepository<QuestionOption, Long> {
}