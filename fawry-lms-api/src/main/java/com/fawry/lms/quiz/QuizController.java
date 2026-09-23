package com.fawry.lms.quiz;

import com.fawry.lms.quiz.dtos.CreateQuizRequest;
import com.fawry.lms.quiz.dtos.QuizResponse;
import com.fawry.lms.quiz.dtos.UpdateQuizRequest;
import com.fawry.lms.quiz.dtos.CreateQuestionRequest;
import com.fawry.lms.quiz.dtos.UpdateQuestionRequest;
import com.fawry.lms.quiz.dtos.QuestionResponse;
import com.fawry.lms.quiz.dtos.QuizDetailResponse;
import com.fawry.lms.quiz.dtos.SubmitQuizRequest;
import com.fawry.lms.quiz.dtos.SubmitQuizResponse;
import com.fawry.lms.user.entities.User;

import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

@RestController
@RequestMapping
@PreAuthorize("hasRole('ADMIN')")
public class QuizController {

    private final QuizService quizService;

    public QuizController(QuizService quizService) {
        this.quizService = quizService;
    }

    @GetMapping("/api/courses/{courseId}/quizzes")
    @PreAuthorize("hasRole('ADMIN') or @authz.isEnrolledOrStaff(authentication.principal, @courseService.getEntity(#courseId))")
    public Page<QuizResponse> list(
            @PathVariable Long courseId,
            @AuthenticationPrincipal User user,
            Pageable pageable) {
        return quizService.list(courseId, user, pageable);
    }

    @GetMapping("/api/quizzes/{id}")
    @PreAuthorize("hasRole('ADMIN') or @authz.isEnrolledOrStaff(authentication.principal, @quizService.getCourse(#id))")
    public QuizDetailResponse detail(
            @PathVariable Long id,
            @AuthenticationPrincipal User user) {
        return quizService.getDetail(id, user);
    }

    @PostMapping("/api/quizzes/{id}/submit")
    @PreAuthorize("hasRole('STUDENT') and @authz.isEnrolledOrStaff(authentication.principal, @quizService.getCourse(#id))")
    public SubmitQuizResponse submit(
            @PathVariable Long id,
            @AuthenticationPrincipal User student,
            @Valid @RequestBody SubmitQuizRequest request) {
        return quizService.submit(id, student, request);
    }

    @PostMapping("/api/courses/{courseId}/quizzes")
    @PreAuthorize("hasRole('ADMIN') or @authz.isOwnerOrAdmin(authentication.principal, @courseService.getEntity(#courseId))")
    public QuizResponse create(@PathVariable Long courseId, @Valid @RequestBody CreateQuizRequest request) {
        return quizService.create(courseId, request);
    }

    @PatchMapping("/api/quizzes/{id}")
    @PreAuthorize("hasRole('ADMIN') or @authz.isOwnerOrAdmin(authentication.principal, @quizService.getCourse(#id))")
    public QuizResponse update(@PathVariable Long id, @RequestBody UpdateQuizRequest request) {
        return quizService.update(id, request);
    }

    @DeleteMapping("/api/quizzes/{id}")
    @PreAuthorize("hasRole('ADMIN') or @authz.isOwnerOrAdmin(authentication.principal, @quizService.getCourse(#id))")
    public void delete(@PathVariable Long id) {
        quizService.delete(id);
    }

    @GetMapping("/api/quizzes/{id}/questions")
    @PreAuthorize("hasRole('ADMIN') or @authz.isOwnerOrAdmin(authentication.principal, @quizService.getCourse(#id))")
    public List<QuestionResponse> getQuestions(@PathVariable Long id) {
        return quizService.getQuestions(id);
    }

    @PostMapping("/api/quizzes/{id}/questions")
    @PreAuthorize("hasRole('ADMIN') or @authz.isOwnerOrAdmin(authentication.principal, @quizService.getCourse(#id))")
    public QuestionResponse addQuestion(
            @PathVariable Long id,
            @Valid @RequestBody CreateQuestionRequest request) {
        return quizService.addQuestion(id, request);
    }

    @PatchMapping("/api/questions/{id}")
    @PreAuthorize("hasRole('ADMIN') or @authz.isOwnerOrAdmin(authentication.principal, @quizService.getQuestionCourse(#id))")
    public QuestionResponse updateQuestion(
            @PathVariable Long id,
            @Valid @RequestBody UpdateQuestionRequest request) {
        return quizService.updateQuestion(id, request);
    }

    @DeleteMapping("/api/questions/{id}")
    @PreAuthorize("hasRole('ADMIN') or @authz.isOwnerOrAdmin(authentication.principal, @quizService.getQuestionCourse(#id))")
    public void deleteQuestion(@PathVariable Long id) {
        quizService.deleteQuestion(id);
    }
}
