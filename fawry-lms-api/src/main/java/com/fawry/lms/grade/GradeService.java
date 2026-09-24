package com.fawry.lms.grade;

import com.fawry.lms.course.EnrollmentRepository;
import com.fawry.lms.course.CourseRepository;
import com.fawry.lms.course.entities.Course;
import com.fawry.lms.course.entities.Enrollment;
import com.fawry.lms.grade.dtos.CourseGradesResponse;
import com.fawry.lms.grade.dtos.QuizGradeResponse;
import com.fawry.lms.grade.dtos.CourseGradeResponse;
import com.fawry.lms.grade.dtos.GradeStudentResponse;
import com.fawry.lms.grade.dtos.GradeQuizResponse;
import com.fawry.lms.quiz.entities.QuizAttempt;
import com.fawry.lms.quiz.repositories.QuizAttemptRepository;
import com.fawry.lms.user.entities.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import jakarta.persistence.EntityNotFoundException;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class GradeService {

    private final EnrollmentRepository enrollmentRepository;
    private final QuizAttemptRepository attemptRepository;
    private final CourseRepository courseRepository;

    public GradeService(
            EnrollmentRepository enrollmentRepository,
            QuizAttemptRepository attemptRepository,
            CourseRepository courseRepository) {
        this.enrollmentRepository = enrollmentRepository;
        this.attemptRepository = attemptRepository;
        this.courseRepository = courseRepository;
    }

    @Transactional(readOnly = true)
    public List<CourseGradesResponse> getMyGrades(User student) {
        List<Enrollment> enrollments = enrollmentRepository.findByStudent(student);
        Map<Long, List<QuizGradeResponse>> gradesByCourse = attemptRepository.findByStudent_Id(student.getId()).stream()
                .filter(attempt -> attempt.getScore() != null)
                .collect(Collectors.groupingBy(
                        attempt -> attempt.getQuiz().getCourse().getId(),
                        Collectors.mapping(this::toQuizGrade, Collectors.toList())));

        return enrollments.stream()
                .map(enrollment -> {
                    var course = enrollment.getCourse();
                    return new CourseGradesResponse(
                            course.getId(),
                            course.getTitle(),
                            course.getCode(),
                            gradesByCourse.getOrDefault(course.getId(), List.of()));
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public Page<CourseGradeResponse> getCourseGrades(Long courseId, Pageable pageable) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new EntityNotFoundException("Course not found."));
        return attemptRepository.findByQuiz_Course_IdAndScoreIsNotNull(courseId, pageable)
                .map(attempt -> new CourseGradeResponse(
                        new GradeStudentResponse(
                                attempt.getStudent().getId(),
                                attempt.getStudent().getFullName(),
                                attempt.getStudent().getEmail()),
                        new GradeQuizResponse(attempt.getQuiz().getId(), attempt.getQuiz().getTitle()),
                        attempt.getScore(),
                        attempt.getTotalQuestions(),
                        attempt.getSubmittedAt()));
    }

    private QuizGradeResponse toQuizGrade(QuizAttempt attempt) {
        return new QuizGradeResponse(
                attempt.getQuiz().getId(),
                attempt.getQuiz().getTitle(),
                attempt.getScore(),
                attempt.getTotalQuestions(),
                attempt.getSubmittedAt());
    }
}
