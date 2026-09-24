package com.fawry.lms.communication;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.fawry.lms.communication.entities.Announcement;
import java.util.List;
import java.util.UUID;

public interface AnnouncementRepository extends JpaRepository<Announcement, Long> {

    Page<Announcement> findByCourse_IdOrderByCreatedAtDesc(Long courseId, Pageable pageable);

    List<Announcement> findByAuthor_IdAndCourse_Instructor_IdOrderByCreatedAtDesc(UUID authorId, UUID instructorId);
}
