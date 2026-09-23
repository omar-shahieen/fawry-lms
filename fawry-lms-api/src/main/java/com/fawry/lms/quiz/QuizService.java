package com.fawry.lms.quiz;

import com.fawry.lms.course.CourseRepository;
import com.fawry.lms.course.entities.Course;
import com.fawry.lms.quiz.dto.CreateQuizRequest;
import com.fawry.lms.quiz.dto.QuizResponse;
import com.fawry.lms.quiz.dto.UpdateQuizRequest;
import com.fawry.lms.quiz.dto.CreateQuestionRequest;
import com.fawry.lms.quiz.dto.UpdateQuestionRequest;
import com.fawry.lms.quiz.dto.QuestionResponse;
import com.fawry.lms.quiz.dto.QuestionOptionRequest;
import com.fawry.lms.quiz.dto.QuestionOptionResponse;
import com.fawry.lms.quiz.entities.Quiz;
import com.fawry.lms.quiz.entities.Question;
import com.fawry.lms.quiz.entities.QuestionOption;
import com.fawry.lms.user.entities.Role;
import com.fawry.lms.user.entities.User;

import jakarta.persistence.EntityNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
public class QuizService {

    private final QuizRepository quizRepository;
    private final CourseRepository courseRepository;
    private final QuestionRepository questionRepository;
    private final QuestionOptionRepository optionRepository;

    public QuizService(
            QuizRepository quizRepository,
            CourseRepository courseRepository,
            QuestionRepository questionRepository,
            QuestionOptionRepository optionRepository) {
        this.quizRepository = quizRepository;
        this.courseRepository = courseRepository;
        this.questionRepository = questionRepository;
        this.optionRepository = optionRepository;
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
    public List<QuestionResponse> getQuestions(Long id) {
        return findQuiz(id).getQuestions().stream().map(this::toQuestionResponse).toList();
    }

    @Transactional
    public QuestionResponse addQuestion(Long quizId, CreateQuestionRequest request) {
        validateOptions(request.options());
        Question question = new Question();
        question.setQuiz(findQuiz(quizId));
        question.setText(request.text());
        question.setOrderIndex(request.orderIndex());
        for (QuestionOptionRequest optionRequest : request.options()) {
            QuestionOption option = new QuestionOption();
            option.setText(optionRequest.text());
            option.setCorrect(optionRequest.isCorrect());
            question.addOption(option);
        }
        return toQuestionResponse(questionRepository.saveAndFlush(question));
    }

    @Transactional
    public QuestionResponse updateQuestion(Long id, UpdateQuestionRequest request) {
        Question question = findQuestion(id);
        if (request.text() != null) question.setText(request.text());
        if (request.orderIndex() != null) question.setOrderIndex(request.orderIndex());
        if (request.options() != null) {
            validateOptions(request.options());
            optionRepository.deleteAll(question.getOptions());
            question.getOptions().clear();
            for (QuestionOptionRequest optionRequest : request.options()) {
                QuestionOption option = new QuestionOption();
                option.setText(optionRequest.text());
                option.setCorrect(optionRequest.isCorrect());
                question.addOption(option);
            }
        }
        return toQuestionResponse(questionRepository.saveAndFlush(question));
    }

    @Transactional
    public void deleteQuestion(Long id) {
        questionRepository.delete(findQuestion(id));
    }

    @Transactional(readOnly = true)
    public Course getQuestionCourse(Long id) {
        return findQuestion(id).getQuiz().getCourse();
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

    private Question findQuestion(Long id) {
        return questionRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Question not found."));
    }

    private void validateOptions(List<QuestionOptionRequest> options) {
        if (options.size() < 2 || options.stream().filter(QuestionOptionRequest::isCorrect).count() != 1) {
            throw new IllegalArgumentException("A question requires at least two options and exactly one correct option.");
        }
    }

    private QuestionResponse toQuestionResponse(Question question) {
        return new QuestionResponse(
                question.getId(),
                question.getText(),
                question.getOrderIndex(),
                question.getOptions().stream()
                        .map(option -> new QuestionOptionResponse(option.getId(), option.getText(), option.isCorrect()))
                        .toList());
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
