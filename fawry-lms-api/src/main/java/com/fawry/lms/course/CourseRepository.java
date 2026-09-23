package com.fawry.lms.course;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.fawry.lms.course.entities.Course;

public interface CourseRepository extends JpaRepository<Course, Long> {

    @Query("""
            select c from Course c
            where c.isActive = true
              and (:search is null or lower(c.title) like lower(concat('%', :search, '%')))
              and (:term is null or c.term = :term)
              and (:code is null or c.code = :code)
            """)
    Page<Course> searchActive(
            @Param("search") String search,
            @Param("term") String term,
            @Param("code") String code,
            Pageable pageable);
}
