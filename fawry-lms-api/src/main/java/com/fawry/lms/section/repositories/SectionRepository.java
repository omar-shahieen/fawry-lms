package com.fawry.lms.section.repositories;

import com.fawry.lms.course.entities.Course;
import com.fawry.lms.section.entities.Section;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SectionRepository extends JpaRepository<Section, Long> {
    List<Section> findByCourseOrderByOrderIndexAsc(Course course);
}