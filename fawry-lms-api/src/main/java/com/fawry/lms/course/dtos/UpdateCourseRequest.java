package com.fawry.lms.course.dtos;

import java.util.UUID;

public record UpdateCourseRequest(
        String title,
        String description,
        String code,
        String term,
        UUID instructorId) {
}
