package com.fawry.lms.section;

import com.fawry.lms.course.CourseRepository;
import com.fawry.lms.course.entities.Course;
import com.fawry.lms.section.dto.CreateSectionRequest;
import com.fawry.lms.section.dto.SectionResponse;
import com.fawry.lms.section.dto.UpdateSectionRequest;
import com.fawry.lms.section.entities.Section;

import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class SectionService {

    private final SectionRepository sectionRepository;
    private final CourseRepository courseRepository;

    public SectionService(SectionRepository sectionRepository, CourseRepository courseRepository) {
        this.sectionRepository = sectionRepository;
        this.courseRepository = courseRepository;
    }

    @Transactional(readOnly = true)
    public List<SectionResponse> list(Long courseId) {
        Course course = findCourse(courseId);
        return sectionRepository.findByCourseOrderByOrderIndexAsc(course).stream().map(this::toResponse).toList();
    }

    @Transactional
    public SectionResponse create(Long courseId, CreateSectionRequest request) {
        Section section = new Section();
        section.setCourse(findCourse(courseId));
        section.setTitle(request.title());
        section.setOrderIndex(request.orderIndex());
        return toResponse(sectionRepository.saveAndFlush(section));
    }

    @Transactional
    public SectionResponse update(Long id, UpdateSectionRequest request) {
        Section section = findSection(id);
        if (request.title() != null) section.setTitle(request.title());
        if (request.orderIndex() != null) section.setOrderIndex(request.orderIndex());
        return toResponse(sectionRepository.saveAndFlush(section));
    }

    @Transactional
    public void delete(Long id) {
        sectionRepository.delete(findSection(id));
    }

    @Transactional(readOnly = true)
    public Course getCourse(Long id) {
        return findSection(id).getCourse();
    }

    private Course findCourse(Long id) {
        return courseRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Course not found."));
    }

    private Section findSection(Long id) {
        return sectionRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Section not found."));
    }

    private SectionResponse toResponse(Section section) {
        return new SectionResponse(section.getId(), section.getCourse().getId(), section.getTitle(), section.getOrderIndex());
    }
}
