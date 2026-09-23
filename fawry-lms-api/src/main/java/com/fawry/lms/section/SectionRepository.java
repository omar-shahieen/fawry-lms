package com.fawry.lms.section;

import com.fawry.lms.course.Course;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SectionRepository extends JpaRepository<Section, Long> {
    List<Section> findByCourseOrderByOrderIndexAsc(Course course);
}