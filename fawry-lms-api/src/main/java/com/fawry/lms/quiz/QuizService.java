package com.fawry.lms.quiz;

import com.fawry.lms.course.CourseRepository;
import com.fawry.lms.course.entities.Course;
import com.fawry.lms.quiz.dtos.CreateQuizRequest;
import com.fawry.lms.quiz.dtos.QuizResponse;
import com.fawry.lms.quiz.dtos.UpdateQuizRequest;
import com.fawry.lms.quiz.dtos.CreateQuestionRequest;
import com.fawry.lms.quiz.dtos.UpdateQuestionRequest;
import com.fawry.lms.quiz.dtos.QuestionResponse;
import com.fawry.lms.quiz.dtos.QuestionOptionRequest;
import com.fawry.lms.quiz.dtos.QuestionOptionResponse;
import com.fawry.lms.quiz.dtos.QuizDetailResponse;
import com.fawry.lms.quiz.dtos.QuizStudentQuestionResponse;
import com.fawry.lms.quiz.dtos.QuizStudentOptionResponse;
import com.fawry.lms.quiz.dtos.QuizAnswerResponse;
import com.fawry.lms.quiz.dtos.SubmitAnswerRequest;
import com.fawry.lms.quiz.dtos.SubmitQuizRequest;
import com.fawry.lms.quiz.dtos.SubmitQuizResponse;
import com.fawry.lms.quiz.dtos.QuizAttemptResponse;
import com.fawry.lms.quiz.entities.Quiz;
import com.fawry.lms.quiz.entities.Question;
import com.fawry.lms.quiz.entities.QuestionOption;
import com.fawry.lms.quiz.entities.QuizAttempt;
import com.fawry.lms.quiz.entities.QuizAnswer;
import com.fawry.lms.quiz.repositories.QuestionOptionRepository;
import com.fawry.lms.quiz.repositories.QuestionRepository;
import com.fawry.lms.quiz.repositories.QuizAnswerRepository;
import com.fawry.lms.quiz.repositories.QuizAttemptRepository;
import com.fawry.lms.quiz.repositories.QuizRepository;
import com.fawry.lms.user.entities.Role;
import com.fawry.lms.user.entities.User;

import jakarta.persistence.EntityNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.time.Instant;
import java.util.Map;
import java.util.stream.Collectors;
import com.fawry.lms.common.ConflictException;

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
        if (request.title() != null)
            quiz.setTitle(request.title());
        if (request.durationMinutes() != null)
            quiz.setDurationMinutes(request.durationMinutes());
        if (request.published() != null)
            quiz.setPublished(request.published());
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
    public SubmitQuizResponse submit(Long quizId, User student, SubmitQuizRequest request) {
        Quiz quiz = findQuiz(quizId);
        if (!quiz.isPublished()) {
            throw new IllegalArgumentException("Quiz is not published.");
        }
        QuizAttempt attempt = attemptRepository.findByQuizIdAndStudentId(quizId, student.getId())
                .orElseThrow(() -> new IllegalArgumentException("Quiz attempt has not been started."));
        if (attempt.getSubmittedAt() != null) {
            throw new ConflictException("Quiz attempt has already been submitted.");
        }
        if (Instant.now().isAfter(attempt.getStartedAt().plusSeconds(quiz.getDurationMinutes().longValue() * 60))) {
            throw new IllegalArgumentException("Quiz attempt has expired.");
        }

        Map<Long, SubmitAnswerRequest> submitted = request.answers().stream()
                .collect(Collectors.toMap(SubmitAnswerRequest::questionId, answer -> answer, (first, second) -> second));
        List<QuizAnswer> answers = new java.util.ArrayList<>();
        int score = 0;
        for (Question question : quiz.getQuestions()) {
            SubmitAnswerRequest answerRequest = submitted.get(question.getId());
            QuestionOption selected = answerRequest == null || answerRequest.selectedOptionId() == null
                    ? null
                    : question.getOptions().stream()
                            .filter(option -> option.getId().equals(answerRequest.selectedOptionId()))
                            .findFirst()
                            .orElseThrow(() -> new IllegalArgumentException("Selected option does not belong to the question."));
            boolean correct = selected != null && selected.isCorrect();
            if (correct) score++;
            QuizAnswer answer = new QuizAnswer();
            answer.setAttempt(attempt);
            answer.setQuestion(question);
            answer.setSelectedOption(selected);
            answer.setCorrect(correct);
            answers.add(answer);
        }
        answerRepository.saveAllAndFlush(answers);
        attempt.setScore(score);
        attempt.setTotalQuestions(quiz.getQuestions().size());
        attempt.setSubmittedAt(Instant.now());
        attemptRepository.saveAndFlush(attempt);
        return new SubmitQuizResponse(
                attempt.getId(),
                score,
                attempt.getTotalQuestions(),
                attempt.getSubmittedAt(),
                answers.stream().map(answer -> new QuizAnswerResponse(
                        answer.getQuestion().getId(),
                        answer.getSelectedOption() == null ? null : answer.getSelectedOption().getId(),
                        answer.isCorrect())).toList());
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
        if (request.text() != null)
            question.setText(request.text());
        if (request.orderIndex() != null)
            question.setOrderIndex(request.orderIndex());
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

    @Transactional(readOnly = true)
    public QuizAttemptResponse getMyAttempt(Long quizId, User student) {
        QuizAttempt attempt = attemptRepository.findByQuizIdAndStudentId(quizId, student.getId())
                .orElseThrow(() -> new EntityNotFoundException("Quiz attempt not found."));
        return toAttemptResponse(attempt);
    }

    @Transactional(readOnly = true)
    public Page<QuizAttemptResponse> listAttempts(Long quizId, Pageable pageable) {
        findQuiz(quizId);
        return attemptRepository.findByQuizId(quizId, pageable).map(this::toAttemptResponse);
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
            throw new IllegalArgumentException(
                    "A question requires at least two options and exactly one correct option.");
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

    private QuizAttemptResponse toAttemptResponse(QuizAttempt attempt) {
        User student = attempt.getStudent();
        return new QuizAttemptResponse(
                attempt.getId(),
                attempt.getQuiz().getId(),
                student.getId(),
                student.getFullName(),
                student.getEmail(),
                attempt.getStartedAt(),
                attempt.getSubmittedAt(),
                attempt.getScore(),
                attempt.getTotalQuestions());
    }
}
