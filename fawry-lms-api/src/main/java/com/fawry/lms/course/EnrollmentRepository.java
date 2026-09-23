package com.fawry.lms.course;

import com.fawry.lms.user.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EnrollmentRepository extends JpaRepository<Enrollment, Long> {

    boolean existsByCourseAndStudent(Course course, User student);
}
