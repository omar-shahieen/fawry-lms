package com.fawry.lms.quiz.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.List;
import com.fawry.lms.course.entities.Course;

import com.fawry.lms.quiz.entities.Quiz;

public interface QuizRepository extends JpaRepository<Quiz, Long> {
    Page<Quiz> findByCourse(Course course, Pageable pageable);

    Page<Quiz> findByCourseAndPublishedTrue(Course course, Pageable pageable);

    List<Quiz> findByCourse_IdAndPublishedTrue(Long courseId);
}
