package com.fawry.lms.course;

import com.fawry.lms.course.dto.CourseResponse;
import com.fawry.lms.course.entities.Course;

import jakarta.persistence.EntityNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CourseService {

    private final CourseRepository courseRepository;

    public CourseService(CourseRepository courseRepository) {
        this.courseRepository = courseRepository;
    }

    @Transactional(readOnly = true)
    public Page<CourseResponse> listActive(String search, String term, String code, Pageable pageable) {
        return courseRepository.searchActive(normalize(search), normalize(term), normalize(code), pageable)
                .map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public CourseResponse getById(Long id) {
        return courseRepository.findById(id)
                .map(this::toResponse)
                .orElseThrow(() -> new EntityNotFoundException("Course not found."));
    }

    private CourseResponse toResponse(Course course) {
        return new CourseResponse(
                course.getId(),
                course.getTitle(),
                course.getDescription(),
                course.getCode(),
                course.getTerm(),
                course.getInstructor().getId(),
                course.getInstructor().getFullName(),
                course.isActive(),
                course.getCreatedAt(),
                course.getUpdatedAt());
    }

    private String normalize(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
