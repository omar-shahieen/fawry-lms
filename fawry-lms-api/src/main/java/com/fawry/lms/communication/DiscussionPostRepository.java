package com.fawry.lms.communication;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DiscussionPostRepository extends JpaRepository<DiscussionPost, Long> {

    Page<DiscussionPost> findByCourse_IdOrderByCreatedAtDesc(Long courseId, Pageable pageable);
}
