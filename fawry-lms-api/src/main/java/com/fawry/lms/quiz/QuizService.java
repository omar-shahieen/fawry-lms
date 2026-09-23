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
import com.fawry.lms.quiz.dto.QuizDetailResponse;
import com.fawry.lms.quiz.dto.QuizStudentQuestionResponse;
import com.fawry.lms.quiz.dto.QuizStudentOptionResponse;
import com.fawry.lms.quiz.dto.QuizAnswerResponse;
import com.fawry.lms.quiz.entities.Quiz;
import com.fawry.lms.quiz.entities.Question;
import com.fawry.lms.quiz.entities.QuestionOption;
import com.fawry.lms.quiz.entities.QuizAttempt;
import com.fawry.lms.user.entities.Role;
import com.fawry.lms.user.entities.User;

import jakarta.persistence.EntityNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.time.Instant;

@Service
public class QuizService {

    private final QuizRepository quizRepository;
    private final CourseRepository courseRepository;
    private final QuestionRepository questionRepository;
    private final QuestionOptionRepository optionRepository;
    private final QuizAttemptRepository attemptRepository;
    private final QuizAnswerRepository answerRepository;

    public QuizService(
            QuizRepository quizRepository,
            CourseRepository courseRepository,
            QuestionRepository questionRepository,
            QuestionOptionRepository optionRepository,
            QuizAttemptRepository attemptRepository,
            QuizAnswerRepository answerRepository) {
        this.quizRepository = quizRepository;
        this.courseRepository = courseRepository;
        this.questionRepository = questionRepository;
        this.optionRepository = optionRepository;
        this.attemptRepository = attemptRepository;
        this.answerRepository = answerRepository;
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
    public QuizDetailResponse getDetail(Long id, User user) {
        Quiz quiz = findQuiz(id);
        QuizAttempt attempt = null;
        if (user.getRole() == Role.STUDENT) {
            if (!quiz.isPublished()) {
                throw new EntityNotFoundException("Quiz not found.");
            }
            attempt = attemptRepository.findByQuizIdAndStudentId(id, user.getId()).orElse(null);
            if (attempt == null) {
                attempt = new QuizAttempt();
                attempt.setQuiz(quiz);
                attempt.setStudent(user);
                attempt.setTotalQuestions(quiz.getQuestions().size());
                attempt = attemptRepository.saveAndFlush(attempt);
            }
        }
        Instant expiresAt = attempt == null ? null
                : attempt.getStartedAt().plusSeconds(quiz.getDurationMinutes().longValue() * 60);
        List<QuizAnswerResponse> answers = attempt == null || attempt.getSubmittedAt() == null
                ? List.of()
                : answerRepository.findByAttempt(attempt).stream()
                        .map(answer -> new QuizAnswerResponse(
                                answer.getQuestion().getId(),
                                answer.getSelectedOption() == null ? null : answer.getSelectedOption().getId(),
                                answer.isCorrect()))
                        .toList();
        return new QuizDetailResponse(
                quiz.getId(),
                quiz.getCourse().getId(),
                quiz.getTitle(),
                quiz.getDurationMinutes(),
                quiz.isPublished(),
                attempt == null ? null : attempt.getStartedAt(),
                expiresAt,
                attempt == null ? null : attempt.getSubmittedAt(),
                attempt == null ? null : attempt.getScore(),
                attempt == null ? null : attempt.getTotalQuestions(),
                quiz.getQuestions().stream().map(this::toStudentQuestionResponse).toList(),
                answers);
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

    private QuizStudentQuestionResponse toStudentQuestionResponse(Question question) {
        return new QuizStudentQuestionResponse(
                question.getId(),
                question.getText(),
                question.getOrderIndex(),
                question.getOptions().stream()
                        .map(option -> new QuizStudentOptionResponse(option.getId(), option.getText()))
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
