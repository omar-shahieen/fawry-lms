package com.fawry.lms.config;

import com.fawry.lms.communication.AnnouncementRepository;
import com.fawry.lms.communication.DiscussionPostRepository;
import com.fawry.lms.communication.entities.Announcement;
import com.fawry.lms.communication.entities.DiscussionPost;
import com.fawry.lms.course.CourseRepository;
import com.fawry.lms.course.EnrollmentRepository;
import com.fawry.lms.course.entities.Course;
import com.fawry.lms.course.entities.Enrollment;
import com.fawry.lms.quiz.entities.Question;
import com.fawry.lms.quiz.entities.QuestionOption;
import com.fawry.lms.quiz.entities.Quiz;
import com.fawry.lms.quiz.entities.QuizAnswer;
import com.fawry.lms.quiz.entities.QuizAttempt;
import com.fawry.lms.quiz.repositories.QuizAnswerRepository;
import com.fawry.lms.quiz.repositories.QuizAttemptRepository;
import com.fawry.lms.quiz.repositories.QuizRepository;
import com.fawry.lms.section.entities.MarkdownContent;
import com.fawry.lms.section.entities.Section;
import com.fawry.lms.section.repositories.MarkdownContentRepository;
import com.fawry.lms.section.repositories.SectionRepository;
import com.fawry.lms.user.entities.Role;
import com.fawry.lms.user.entities.User;
import com.fawry.lms.user.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Component
@ConditionalOnProperty(prefix = "lms.seed", name = "enabled", havingValue = "true", matchIfMissing = true)
public class DataSeeder implements CommandLineRunner {

    private static final String INSTRUCTOR_PASSWORD = "Instructor123!";
    private static final String STUDENT_PASSWORD = "Student123!";
    private static final List<String> QUESTION_TEXTS = List.of(
            "Which statement best describes the main concept?",
            "Which choice is the correct application of the concept?",
            "What is the best next step when using this concept?");

    private final UserRepository userRepository;
    private final CourseRepository courseRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final SectionRepository sectionRepository;
    private final MarkdownContentRepository contentRepository;
    private final QuizRepository quizRepository;
    private final QuizAttemptRepository attemptRepository;
    private final QuizAnswerRepository answerRepository;
    private final AnnouncementRepository announcementRepository;
    private final DiscussionPostRepository discussionPostRepository;
    private final PasswordEncoder passwordEncoder;
    private final String adminEmail;
    private final String adminPassword;

    public DataSeeder(
            UserRepository userRepository,
            CourseRepository courseRepository,
            EnrollmentRepository enrollmentRepository,
            SectionRepository sectionRepository,
            MarkdownContentRepository contentRepository,
            QuizRepository quizRepository,
            QuizAttemptRepository attemptRepository,
            QuizAnswerRepository answerRepository,
            AnnouncementRepository announcementRepository,
            DiscussionPostRepository discussionPostRepository,
            PasswordEncoder passwordEncoder,
            @Value("${ADMIN_SEED_EMAIL:admin@lms.com}") String adminEmail,
            @Value("${ADMIN_SEED_PASSWORD:Admin123!}") String adminPassword) {
        this.userRepository = userRepository;
        this.courseRepository = courseRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.sectionRepository = sectionRepository;
        this.contentRepository = contentRepository;
        this.quizRepository = quizRepository;
        this.attemptRepository = attemptRepository;
        this.answerRepository = answerRepository;
        this.announcementRepository = announcementRepository;
        this.discussionPostRepository = discussionPostRepository;
        this.passwordEncoder = passwordEncoder;
        this.adminEmail = adminEmail;
        this.adminPassword = adminPassword;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (userRepository.count() != 0) {
            return;
        }

        User admin = createUser("LMS Administrator", adminEmail, Role.ADMIN, adminPassword);
        List<User> instructors = List.of(
                createUser("Amina Hassan", "instructor1@lms.com", Role.INSTRUCTOR, INSTRUCTOR_PASSWORD),
                createUser("Omar Khaled", "instructor2@lms.com", Role.INSTRUCTOR, INSTRUCTOR_PASSWORD));
        List<User> students = new ArrayList<>();
        for (int i = 1; i <= 6; i++) {
            students.add(createUser("Student " + i, "student" + i + "@lms.com", Role.STUDENT,
                    STUDENT_PASSWORD));
        }
        userRepository.saveAllAndFlush(concat(admin, instructors, students));

        List<Course> courses = createCourses(instructors);
        createEnrollments(courses, students);
        createSectionsAndContent(courses);
        List<Quiz> quizzes = createQuizzes(courses);
        createAttemptsAndAnswers(quizzes, students);
        createAnnouncementsAndDiscussions(courses, instructors, students);
    }

    private User createUser(String name, String email, Role role, String rawPassword) {
        User user = new User();
        user.setFullName(name);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(rawPassword));
        user.setRole(role);
        user.setProfilePictureUrl("https://api.dicebear.com/9.x/avataaars/svg?seed=" + email);
        return user;
    }

    private List<Course> createCourses(List<User> instructors) {
        String[] titles = {"Introduction to Computer Science", "Database Systems"};
        String[] codes = {"CS101", "CS202"};
        List<Course> courses = new ArrayList<>();
        for (int i = 0; i < instructors.size(); i++) {
            Course course = new Course();
            course.setTitle(titles[i]);
            course.setDescription("Sample course materials and activities for " + titles[i] + ".");
            course.setCode(codes[i]);
            course.setTerm("Fall 2026");
            course.setInstructor(instructors.get(i));
            courses.add(course);
        }
        return courseRepository.saveAllAndFlush(courses);
    }

    private void createEnrollments(List<Course> courses, List<User> students) {
        List<Enrollment> enrollments = new ArrayList<>();
        for (int i = 0; i < students.size(); i++) {
            Course course = courses.get(i % courses.size());
            enrollments.add(enrollment(course, students.get(i)));
            if (i < 2) {
                enrollments.add(enrollment(courses.get(1 - (i % courses.size())), students.get(i)));
            }
        }
        enrollmentRepository.saveAllAndFlush(enrollments);
    }

    private Enrollment enrollment(Course course, User student) {
        Enrollment enrollment = new Enrollment();
        enrollment.setCourse(course);
        enrollment.setStudent(student);
        return enrollment;
    }

    private void createSectionsAndContent(List<Course> courses) {
        for (Course course : courses) {
            for (int i = 1; i <= 2; i++) {
                Section section = new Section();
                section.setCourse(course);
                section.setTitle(i == 1 ? "Getting Started" : "Core Concepts");
                section.setOrderIndex(i);
                sectionRepository.saveAndFlush(section);

                MarkdownContent content = new MarkdownContent();
                content.setSection(section);
                content.setTitle(section.getTitle() + " Notes");
                content.setBody("# " + section.getTitle() + "\n\nWelcome to " + course.getTitle()
                        + ". Review the examples and complete the practice quiz.");
                contentRepository.save(content);
            }
        }
    }

    private List<Quiz> createQuizzes(List<Course> courses) {
        List<Quiz> quizzes = new ArrayList<>();
        for (Course course : courses) {
            Quiz quiz = new Quiz();
            quiz.setCourse(course);
            quiz.setTitle(course.getTitle() + " Checkpoint Quiz");
            quiz.setDurationMinutes(30);
            quiz.setPublished(true);
            for (int i = 0; i < QUESTION_TEXTS.size(); i++) {
                Question question = new Question();
                question.setText(QUESTION_TEXTS.get(i));
                question.setOrderIndex(i + 1);
                question.addOption(option("Apply the course concept correctly", true));
                question.addOption(option("Ignore the stated requirements", false));
                quiz.addQuestion(question);
            }
            quizzes.add(quizRepository.saveAndFlush(quiz));
        }
        return quizzes;
    }

    private QuestionOption option(String text, boolean correct) {
        QuestionOption option = new QuestionOption();
        option.setText(text);
        option.setCorrect(correct);
        return option;
    }

    private void createAttemptsAndAnswers(List<Quiz> quizzes, List<User> students) {
        Instant now = Instant.now();
        List<QuizAttempt> attempts = new ArrayList<>();
        attempts.add(createAttempt(quizzes.get(0), students.get(0), now.minusSeconds(86_400)));
        attempts.add(createAttempt(quizzes.get(0), students.get(1), now.minusSeconds(43_200)));
        attempts.add(createAttempt(quizzes.get(1), students.get(0), now.minusSeconds(21_600)));
        attemptRepository.saveAllAndFlush(attempts);

        List<QuizAnswer> answers = new ArrayList<>();
        for (QuizAttempt attempt : attempts) {
            for (Question question : attempt.getQuiz().getQuestions()) {
                QuestionOption correctOption = question.getOptions().stream()
                        .filter(QuestionOption::isCorrect)
                        .findFirst()
                        .orElseThrow();
                QuizAnswer answer = new QuizAnswer();
                answer.setAttempt(attempt);
                answer.setQuestion(question);
                answer.setSelectedOption(correctOption);
                answer.setCorrect(true);
                answers.add(answer);
            }
        }
        answerRepository.saveAll(answers);
    }

    private QuizAttempt createAttempt(Quiz quiz, User student, Instant submittedAt) {
        QuizAttempt attempt = new QuizAttempt();
        attempt.setQuiz(quiz);
        attempt.setStudent(student);
        attempt.setStartedAt(submittedAt.minusSeconds(quiz.getDurationMinutes() * 60L));
        attempt.setSubmittedAt(submittedAt);
        attempt.setScore(quiz.getQuestions().size());
        attempt.setTotalQuestions(quiz.getQuestions().size());
        return attempt;
    }

    private void createAnnouncementsAndDiscussions(List<Course> courses, List<User> instructors,
            List<User> students) {
        for (int i = 0; i < courses.size(); i++) {
            Course course = courses.get(i);
            Announcement announcement = new Announcement();
            announcement.setCourse(course);
            announcement.setAuthor(instructors.get(i));
            announcement.setTitle("Welcome to " + course.getTitle());
            announcement.setBody("Course resources and the first quiz are now available.");
            announcementRepository.save(announcement);

            Announcement reminder = new Announcement();
            reminder.setCourse(course);
            reminder.setAuthor(instructors.get(i));
            reminder.setTitle("Checkpoint quiz reminder");
            reminder.setBody("Review the course notes before starting the published checkpoint quiz.");
            announcementRepository.save(reminder);

            DiscussionPost question = new DiscussionPost();
            question.setCourse(course);
            question.setAuthor(students.get(i));
            question.setTitle("Getting started");
            question.setBody("What should I review before the checkpoint quiz?");
            question = discussionPostRepository.saveAndFlush(question);

            DiscussionPost reply = new DiscussionPost();
            reply.setCourse(course);
            reply.setAuthor(instructors.get(i));
            reply.setBody("Start with the Core Concepts section and its examples.");
            reply.setParentPost(question);
            discussionPostRepository.save(reply);
        }
    }

    private List<User> concat(User admin, List<User> instructors, List<User> students) {
        List<User> users = new ArrayList<>();
        users.add(admin);
        users.addAll(instructors);
        users.addAll(students);
        return users;
    }
}
