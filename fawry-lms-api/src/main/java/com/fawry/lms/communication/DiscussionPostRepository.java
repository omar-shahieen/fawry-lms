package com.fawry.lms.communication;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.fawry.lms.communication.entities.DiscussionPost;

public interface DiscussionPostRepository extends JpaRepository<DiscussionPost, Long> {

    Page<DiscussionPost> findByCourse_IdOrderByCreatedAtDesc(Long courseId, Pageable pageable);

    Page<DiscussionPost> findByCourse_IdAndParentPostIsNullOrderByCreatedAtDesc(Long courseId, Pageable pageable);

    java.util.List<DiscussionPost> findByParentPost_IdOrderByCreatedAtAsc(Long parentPostId);
}
