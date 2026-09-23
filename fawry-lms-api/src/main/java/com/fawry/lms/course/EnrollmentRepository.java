package com.fawry.lms.course;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.fawry.lms.course.entities.Course;
import com.fawry.lms.course.entities.Enrollment;
import com.fawry.lms.user.entities.User;

import java.util.List;

public interface EnrollmentRepository extends JpaRepository<Enrollment, Long> {

    boolean existsByCourseAndStudent(Course course, User student);

    List<Enrollment> findByStudent(User student);

    Page<Enrollment> findByCourse(Course course, Pageable pageable);
}
