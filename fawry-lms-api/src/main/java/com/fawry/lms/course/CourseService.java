package com.fawry.lms.course;

import com.fawry.lms.course.dto.CourseResponse;
import com.fawry.lms.course.entities.Course;
import com.fawry.lms.course.dto.CreateCourseRequest;
import com.fawry.lms.course.dto.UpdateCourseRequest;
import com.fawry.lms.user.UserRepository;
import com.fawry.lms.user.entities.Role;
import com.fawry.lms.user.entities.User;

import jakarta.persistence.EntityNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.UUID;

@Service
public class CourseService {

    private final CourseRepository courseRepository;
    private final UserRepository userRepository;

    public CourseService(CourseRepository courseRepository, UserRepository userRepository) {
        this.courseRepository = courseRepository;
        this.userRepository = userRepository;
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

    public UUID getInstructorId(Long id) {
        return findCourse(id).getInstructor().getId();
    }

    @Transactional
    public CourseResponse create(CreateCourseRequest request) {
        Course course = new Course();
        course.setTitle(request.title());
        course.setDescription(request.description());
        course.setCode(request.code());
        course.setTerm(request.term());
        course.setInstructor(findInstructor(request.instructorId()));
        return toResponse(courseRepository.saveAndFlush(course));
    }

    @Transactional
    public CourseResponse update(Long id, UpdateCourseRequest request) {
        Course course = findCourse(id);
        if (request.title() != null) course.setTitle(request.title());
        if (request.description() != null) course.setDescription(request.description());
        if (request.code() != null) course.setCode(request.code());
        if (request.term() != null) course.setTerm(request.term());
        if (request.instructorId() != null) course.setInstructor(findInstructor(request.instructorId()));
        return toResponse(courseRepository.saveAndFlush(course));
    }

    @Transactional
    public CourseResponse deactivate(Long id) {
        Course course = findCourse(id);
        course.setActive(false);
        return toResponse(courseRepository.saveAndFlush(course));
    }

    private Course findCourse(Long id) {
        return courseRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Course not found."));
    }

    private User findInstructor(UUID id) {
        return userRepository.findById(id)
                .filter(user -> user.getRole() == Role.INSTRUCTOR)
                .orElseThrow(() -> new IllegalArgumentException("Course instructor must have INSTRUCTOR role."));
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
