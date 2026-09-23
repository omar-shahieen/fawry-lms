package com.fawry.lms.communication;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AnnouncementRepository extends JpaRepository<Announcement, Long> {

    Page<Announcement> findByCourse_IdOrderByCreatedAtDesc(Long courseId, Pageable pageable);
}
