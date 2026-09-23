package com.fawry.lms.quiz;

import com.fawry.lms.course.CourseRepository;
import com.fawry.lms.course.entities.Course;
import com.fawry.lms.quiz.dto.CreateQuizRequest;
import com.fawry.lms.quiz.dto.QuizResponse;
import com.fawry.lms.quiz.dto.UpdateQuizRequest;
import com.fawry.lms.quiz.entities.Quiz;
import com.fawry.lms.user.entities.Role;
import com.fawry.lms.user.entities.User;

import jakarta.persistence.EntityNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class QuizService {

    private final QuizRepository quizRepository;
    private final CourseRepository courseRepository;

    public QuizService(QuizRepository quizRepository, CourseRepository courseRepository) {
        this.quizRepository = quizRepository;
        this.courseRepository = courseRepository;
    }

    @Transactional(readOnly = true)
    public Page<QuizResponse> list(Long courseId, User user, Pageable pageable) {
        Course course = findCourse(courseId);
        Page<Quiz> quizzes = user.getRole() == Role.STUDENT
                ? quizRepository.findByCourseAndPublishedTrue(course, pageable)
                : quizRepository.findByCourse(course, pageable);
        return quizzes.map(this::toResponse);
    }

    @Transactional
    public QuizResponse create(Long courseId, CreateQuizRequest request) {
        Quiz quiz = new Quiz();
        quiz.setCourse(findCourse(courseId));
        quiz.setTitle(request.title());
        quiz.setDurationMinutes(request.durationMinutes());
        quiz.setPublished(request.published());
        return toResponse(quizRepository.saveAndFlush(quiz));
    }

    @Transactional
    public QuizResponse update(Long id, UpdateQuizRequest request) {
        Quiz quiz = findQuiz(id);
        if (request.title() != null) quiz.setTitle(request.title());
        if (request.durationMinutes() != null) quiz.setDurationMinutes(request.durationMinutes());
        if (request.published() != null) quiz.setPublished(request.published());
        return toResponse(quizRepository.saveAndFlush(quiz));
    }

    @Transactional
    public void delete(Long id) {
        quizRepository.delete(findQuiz(id));
    }

    @Transactional(readOnly = true)
    public Course getCourse(Long id) {
        return findQuiz(id).getCourse();
    }

    private Course findCourse(Long id) {
        return courseRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Course not found."));
    }

    private Quiz findQuiz(Long id) {
        return quizRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Quiz not found."));
    }

    private QuizResponse toResponse(Quiz quiz) {
        return new QuizResponse(
                quiz.getId(),
                quiz.getCourse().getId(),
                quiz.getTitle(),
                quiz.getDurationMinutes(),
                quiz.isPublished(),
                quiz.getCreatedAt(),
                quiz.getUpdatedAt());
    }
}
