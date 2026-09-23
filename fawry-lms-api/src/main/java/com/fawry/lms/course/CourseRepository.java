package com.fawry.lms.course;

import org.springframework.data.jpa.repository.JpaRepository;

import com.fawry.lms.course.entities.Course;

public interface CourseRepository extends JpaRepository<Course, Long> {
}