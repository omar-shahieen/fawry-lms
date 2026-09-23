package com.fawry.lms.course.dto;

import java.util.UUID;

public record UpdateCourseRequest(
                String title,
                String description,
                String code,
                String term,
                UUID instructorId) {
}
