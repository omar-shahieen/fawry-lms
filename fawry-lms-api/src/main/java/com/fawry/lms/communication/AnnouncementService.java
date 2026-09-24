package com.fawry.lms.communication;

import com.fawry.lms.communication.dtos.AnnouncementResponse;
import com.fawry.lms.communication.dtos.CreateAnnouncementRequest;
import com.fawry.lms.communication.dtos.UpdateAnnouncementRequest;
import com.fawry.lms.communication.entities.Announcement;
import com.fawry.lms.course.CourseRepository;
import com.fawry.lms.course.entities.Course;
import com.fawry.lms.user.entities.User;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AnnouncementService {

    private final AnnouncementRepository announcementRepository;
    private final CourseRepository courseRepository;

    public AnnouncementService(AnnouncementRepository announcementRepository, CourseRepository courseRepository) {
        this.announcementRepository = announcementRepository;
        this.courseRepository = courseRepository;
    }

    @Transactional(readOnly = true)
    public Page<AnnouncementResponse> list(Long courseId, Pageable pageable) {
        findCourse(courseId);
        return announcementRepository.findByCourse_IdOrderByCreatedAtDesc(courseId, pageable).map(this::toResponse);
    }

    @Transactional
    public AnnouncementResponse create(Long courseId, User author, CreateAnnouncementRequest request) {
        Announcement announcement = new Announcement();
        announcement.setCourse(findCourse(courseId));
        announcement.setAuthor(author);
        announcement.setTitle(request.title());
        announcement.setBody(request.body());
        return toResponse(announcementRepository.saveAndFlush(announcement));
    }

    @Transactional
    public AnnouncementResponse update(Long id, UpdateAnnouncementRequest request) {
        Announcement announcement = findAnnouncement(id);
        announcement.setTitle(request.title());
        announcement.setBody(request.body());
        return toResponse(announcementRepository.saveAndFlush(announcement));
    }

    @Transactional
    public void delete(Long id) {
        announcementRepository.delete(findAnnouncement(id));
    }

    @Transactional(readOnly = true)
    public Course getCourse(Long id) {
        return findAnnouncement(id).getCourse();
    }

    private Course findCourse(Long id) {
        return courseRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Course not found."));
    }

    private Announcement findAnnouncement(Long id) {
        return announcementRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Announcement not found."));
    }

    private AnnouncementResponse toResponse(Announcement announcement) {
        return new AnnouncementResponse(
                announcement.getId(),
                announcement.getCourse().getId(),
                announcement.getAuthor().getId(),
                announcement.getAuthor().getFullName(),
                announcement.getTitle(),
                announcement.getBody(),
                announcement.getCreatedAt(),
                announcement.getUpdatedAt());
    }
}
