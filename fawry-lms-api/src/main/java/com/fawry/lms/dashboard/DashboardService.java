package com.fawry.lms.dashboard;

import com.fawry.lms.course.EnrollmentRepository;
import com.fawry.lms.course.CourseRepository;
import com.fawry.lms.course.entities.Enrollment;
import com.fawry.lms.communication.AnnouncementRepository;
import com.fawry.lms.communication.entities.Announcement;
import com.fawry.lms.dashboard.dtos.InstructorAnnouncementResponse;
import com.fawry.lms.dashboard.dtos.InstructorCourseSummaryResponse;
import com.fawry.lms.dashboard.dtos.InstructorDashboardResponse;
import com.fawry.lms.dashboard.dtos.StudentDashboardCourseResponse;
import com.fawry.lms.dashboard.dtos.StudentDashboardQuizResponse;
import com.fawry.lms.quiz.entities.Quiz;
import com.fawry.lms.quiz.entities.QuizAttempt;
import com.fawry.lms.quiz.repositories.QuizAttemptRepository;
import com.fawry.lms.quiz.repositories.QuizRepository;
import com.fawry.lms.user.entities.User;
import com.fawry.lms.user.entities.Role;
import com.fawry.lms.user.repositories.UserRepository;
import com.fawry.lms.dashboard.dtos.AdminDashboardResponse;
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
    private final CourseRepository courseRepository;
    private final AnnouncementRepository announcementRepository;
    private final UserRepository userRepository;

    public DashboardService(
            EnrollmentRepository enrollmentRepository,
            QuizRepository quizRepository,
            QuizAttemptRepository attemptRepository,
            CourseRepository courseRepository,
            AnnouncementRepository announcementRepository,
            UserRepository userRepository) {
        this.enrollmentRepository = enrollmentRepository;
        this.quizRepository = quizRepository;
        this.attemptRepository = attemptRepository;
        this.courseRepository = courseRepository;
        this.announcementRepository = announcementRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<StudentDashboardCourseResponse> getStudentDashboard(User student) {
        Map<Long, QuizAttempt> attemptsByQuiz = attemptRepository.findByStudent_Id(student.getId()).stream()
                .collect(Collectors.toMap(attempt -> attempt.getQuiz().getId(), Function.identity()));

        return enrollmentRepository.findByStudent(student).stream()
                .map(enrollment -> toCourseResponse(enrollment, attemptsByQuiz))
                .toList();
    }

    @Transactional(readOnly = true)
    public InstructorDashboardResponse getInstructorDashboard(User instructor) {
        var courses = courseRepository.findByInstructor_Id(instructor.getId());
        Map<Long, List<QuizAttempt>> attemptsByCourse = attemptRepository
                .findByQuiz_Course_Instructor_IdAndScoreIsNotNull(instructor.getId()).stream()
                .collect(Collectors.groupingBy(attempt -> attempt.getQuiz().getCourse().getId()));
        List<InstructorCourseSummaryResponse> summaries = courses.stream()
                .map(course -> toCourseSummary(course.getId(), course.getTitle(),
                        attemptsByCourse.getOrDefault(course.getId(), List.of())))
                .toList();
        List<InstructorAnnouncementResponse> announcements = announcementRepository
                .findByAuthor_IdAndCourse_Instructor_IdOrderByCreatedAtDesc(instructor.getId(), instructor.getId())
                .stream().map(this::toInstructorAnnouncement).toList();
        return new InstructorDashboardResponse(summaries, announcements);
    }

    @Transactional(readOnly = true)
    public AdminDashboardResponse getAdminDashboard() {
        Map<Role, Long> userCounts = new java.util.EnumMap<>(Role.class);
        for (Role role : Role.values()) {
            userCounts.put(role, userRepository.countByRole(role));
        }
        return new AdminDashboardResponse(
                userCounts,
                courseRepository.count(),
                enrollmentRepository.count());
    }

    private InstructorCourseSummaryResponse toCourseSummary(
            Long courseId, String courseName, List<QuizAttempt> attempts) {
        Double averageScore = attempts.isEmpty()
                ? null
                : attempts.stream().mapToInt(QuizAttempt::getScore).average().orElseThrow();
        return new InstructorCourseSummaryResponse(courseId, courseName, attempts.size(), averageScore);
    }

    private InstructorAnnouncementResponse toInstructorAnnouncement(Announcement announcement) {
        return new InstructorAnnouncementResponse(
                announcement.getId(),
                announcement.getCourse().getId(),
                announcement.getTitle(),
                announcement.getBody(),
                announcement.getCreatedAt());
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
