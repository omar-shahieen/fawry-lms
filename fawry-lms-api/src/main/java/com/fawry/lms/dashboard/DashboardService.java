package com.fawry.lms.dashboard;

import com.fawry.lms.course.EnrollmentRepository;
import com.fawry.lms.course.entities.Enrollment;
import com.fawry.lms.dashboard.dtos.StudentDashboardCourseResponse;
import com.fawry.lms.dashboard.dtos.StudentDashboardQuizResponse;
import com.fawry.lms.quiz.entities.Quiz;
import com.fawry.lms.quiz.entities.QuizAttempt;
import com.fawry.lms.quiz.repositories.QuizAttemptRepository;
import com.fawry.lms.quiz.repositories.QuizRepository;
import com.fawry.lms.user.entities.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class DashboardService {

    private final EnrollmentRepository enrollmentRepository;
    private final QuizRepository quizRepository;
    private final QuizAttemptRepository attemptRepository;

    public DashboardService(
            EnrollmentRepository enrollmentRepository,
            QuizRepository quizRepository,
            QuizAttemptRepository attemptRepository) {
        this.enrollmentRepository = enrollmentRepository;
        this.quizRepository = quizRepository;
        this.attemptRepository = attemptRepository;
    }

    @Transactional(readOnly = true)
    public List<StudentDashboardCourseResponse> getStudentDashboard(User student) {
        Map<Long, QuizAttempt> attemptsByQuiz = attemptRepository.findByStudent_Id(student.getId()).stream()
                .collect(Collectors.toMap(attempt -> attempt.getQuiz().getId(), Function.identity()));

        return enrollmentRepository.findByStudent(student).stream()
                .map(enrollment -> toCourseResponse(enrollment, attemptsByQuiz))
                .toList();
    }

    private StudentDashboardCourseResponse toCourseResponse(
            Enrollment enrollment,
            Map<Long, QuizAttempt> attemptsByQuiz) {
        List<StudentDashboardQuizResponse> quizzes = quizRepository
                .findByCourse_IdAndPublishedTrue(enrollment.getCourse().getId()).stream()
                .map(quiz -> toQuizResponse(quiz, attemptsByQuiz.get(quiz.getId())))
                .toList();
        return new StudentDashboardCourseResponse(
                enrollment.getCourse().getId(),
                enrollment.getCourse().getTitle(),
                quizzes);
    }

    private StudentDashboardQuizResponse toQuizResponse(Quiz quiz, QuizAttempt attempt) {
        return new StudentDashboardQuizResponse(
                quiz.getId(),
                quiz.getTitle(),
                attempt != null,
                attempt == null ? null : attempt.getScore());
    }
}
