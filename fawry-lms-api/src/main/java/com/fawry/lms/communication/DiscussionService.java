package com.fawry.lms.communication;

import com.fawry.lms.communication.dtos.CreateDiscussionPostRequest;
import com.fawry.lms.communication.dtos.DiscussionPostResponse;
import com.fawry.lms.communication.dtos.DiscussionReplyResponse;
import com.fawry.lms.communication.entities.DiscussionPost;
import com.fawry.lms.course.CourseRepository;
import com.fawry.lms.course.entities.Course;
import com.fawry.lms.user.entities.User;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DiscussionService {

    private final DiscussionPostRepository discussionPostRepository;
    private final CourseRepository courseRepository;

    public DiscussionService(DiscussionPostRepository discussionPostRepository, CourseRepository courseRepository) {
        this.discussionPostRepository = discussionPostRepository;
        this.courseRepository = courseRepository;
    }

    @Transactional(readOnly = true)
    public Page<DiscussionPostResponse> list(Long courseId, Pageable pageable) {
        findCourse(courseId);
        return discussionPostRepository
                .findByCourse_IdAndParentPostIsNullOrderByCreatedAtDesc(courseId, pageable)
                .map(this::toResponse);
    }

    @Transactional
    public DiscussionPostResponse create(Long courseId, User author, CreateDiscussionPostRequest request) {
        DiscussionPost post = new DiscussionPost();
        post.setCourse(findCourse(courseId));
        post.setAuthor(author);
        post.setTitle(request.title());
        post.setBody(request.body());
        return toResponse(discussionPostRepository.saveAndFlush(post));
    }

    private Course findCourse(Long id) {
        return courseRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Course not found."));
    }

    private DiscussionPostResponse toResponse(DiscussionPost post) {
        return new DiscussionPostResponse(
                post.getId(),
                post.getCourse().getId(),
                post.getAuthor().getId(),
                post.getAuthor().getFullName(),
                post.getTitle(),
                post.getBody(),
                post.getCreatedAt(),
                post.getUpdatedAt(),
                discussionPostRepository.findByParentPost_IdOrderByCreatedAtAsc(post.getId()).stream()
                        .map(this::toReplyResponse)
                        .toList());
    }

    private DiscussionReplyResponse toReplyResponse(DiscussionPost reply) {
        return new DiscussionReplyResponse(
                reply.getId(),
                reply.getAuthor().getId(),
                reply.getAuthor().getFullName(),
                reply.getBody(),
                reply.getCreatedAt(),
                reply.getUpdatedAt());
    }
}
